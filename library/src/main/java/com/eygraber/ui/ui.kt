package com.eygraber.ui

import android.os.Bundle
import android.util.ArrayMap
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

private val HANDLED_ON_STACK_ENTRY_POPPED: OnStackEntryPopped = { true }

private val DEFAULT_ERROR_LISTENER = object : UiTransactions.CommitErrorListener {
    override fun onError(t: Throwable) {}
}

interface UiManager {
    companion object {
        fun getImpl(
                activity: FragmentActivity,
                commitErrorListener: UiTransactions.CommitErrorListener
        ): UiManager = UiManagerImpl(activity, commitErrorListener)
    }

    val onBackPressListener: OnBackPressListener
    val uiStackPersistenceManager: UiStackPersistenceManager
    val uiStackState: UiStackState
    val uiTransactions: UiTransactions
}

private class UiManagerImpl(
        private val activity: FragmentActivity,
        private val commitErrorListener: UiTransactions.CommitErrorListener = DEFAULT_ERROR_LISTENER
) : UiManager,
        UiTransactions,
        UiStackPersistenceManager,
        OnBackPressListener,
        UiStackState {

    companion object {
        private const val STACK_KEY = "stack"
    }

    override val onBackPressListener = this
    override val uiStackPersistenceManager = this
    override val uiStackState = this
    override val uiTransactions = this

    private val manager by lazy(LazyThreadSafetyMode.NONE) { activity.supportFragmentManager }

    private val stack: Deque<StackEntry> = ArrayDeque()

    private var isInCommit: Boolean = false

    /*
    UiTransactions
     */
    override fun commit(transaction: Transaction.() -> Unit) {
        if (isInCommit) {
            throw IllegalStateException("Cannot call commit while in a commit")
        }

        isInCommit = true

        var didStackChange = false
        try {
            manager.beginTransaction().also { fragmentTransaction ->
                fragmentTransaction.disallowAddToBackStack()
                UiTransaction(fragmentTransaction, manager).also {
                    it.transaction()
                    if (it.sharedElementTransitionSourceSet && !it.sharedElementTransitionTargetSet) {
                        throw IllegalStateException("The target fragment for the shared element transition was not set")
                    }
                    didStackChange = it.didStackChange
                }
            }.commitNow()
        } catch (t: Throwable) {
            if (commitErrorListener == DEFAULT_ERROR_LISTENER) {
                throw t
            } else {
                commitErrorListener.onError(t)
            }
        }

        isInCommit = false

        if (didStackChange) {
            stackChangeListeners.forEach(OnStackChangeListener::onStackChanged)
        }
    }

    override fun clearStack(onStackEntryPopped: OnStackEntryPopped) {
        commit { internalPop(null, true, onStackEntryPopped) }
    }

    override fun Transaction.push(tag: String, pushTransaction: PushTransaction.() -> Unit) {
        val transactions = PushTransaction().also(pushTransaction)

        transactions.sharedElements?.let(::setSharedElements)

        val popEntries = transactions
                .entries
                .let { entries ->
                    // record the pop entries first before we perform the actual op (mostly important for REMOVE)
                    val popEntries = entries
                        .reversed()
                        .mapTo(ArrayList(transactions.entries.size)) {
                            if(it.op == TransactionOp.REMOVE) {
                                val fragment = manager.findFragmentByTag(it.tag)
                                    ?: throw IllegalArgumentException("No fragment found for tag ${it.tag}")

                                PopEntry(
                                    it.tag, it.popAnimationId,
                                    it.op.reverse(), it.markForSharedElementTransition,
                                    id = fragment.id,
                                    fqn = fragment.javaClass.name,
                                    state = manager.saveFragmentInstanceState(fragment),
                                    args = fragment.arguments
                                )
                            }
                            else {
                                PopEntry(
                                    it.tag, it.popAnimationId,
                                    it.op.reverse(), it.markForSharedElementTransition
                                )
                            }
                        }

                    // then perform the actual op once the pre-op state has been recorded (mostly important for REMOVE)
                    entries.onEach {
                        when (it.op) {
                            TransactionOp.ADD -> add(it.id!!, it.fragment!!, it.tag, it.animationId, it.markForSharedElementTransition)
                            TransactionOp.ATTACH -> attach(it.tag, it.animationId, it.markForSharedElementTransition)
                            TransactionOp.SHOW -> show(it.tag, it.animationId, it.markForSharedElementTransition)
                            TransactionOp.HIDE -> hide(it.tag, it.animationId, it.markForSharedElementTransition)
                            TransactionOp.DETACH -> detach(it.tag, it.animationId, it.markForSharedElementTransition)
                            TransactionOp.REMOVE -> remove(it.tag, it.animationId, it.markForSharedElementTransition)
                        }
                    }

                    // return the pop entries
                    popEntries
                }

        val (popSourceNamesForSharedElementTransition, popTargetNamesForSharedElementTransition) =
                transactions.sharedElements?.let { sharedElements ->
                    val (names, views) = sharedElements.unzip()

                    names to views.toTransitionNames()
                } ?: null to null

        val entry =
                StackEntry(tag, popEntries, popSourceNamesForSharedElementTransition, popTargetNamesForSharedElementTransition)

        stack.push(entry)

        (this as TransactionBase).didStackChange = true
    }

    override fun Transaction.pop(tag: String, inclusive: Boolean, onStackEntryPopped: OnStackEntryPopped) {
        internalPop(tag, inclusive, onStackEntryPopped)
    }

    override fun Transaction.popTop(onStackEntryPopped: OnStackEntryPopped) {
        stack.peek()?.tag?.let { tag ->
            internalPop(tag, true, onStackEntryPopped)
        }
    }

    private fun Transaction.internalPop(
            tag: String?,
            inclusive: Boolean,
            onStackEntryPopped: OnStackEntryPopped = HANDLED_ON_STACK_ENTRY_POPPED
    ) {
        fun StackEntry.popOp() {
            var overrideTargetEnterTransition: Any? = null

            entries
                    .forEach { entry ->
                        val isPotentialSharedElementTransitionSource =
                                when (entry.popOp) {
                                    TransactionOp.HIDE, TransactionOp.DETACH, TransactionOp.REMOVE -> true
                                    else -> false
                                }

                        val isSharedElementTransitionSource =
                                isPotentialSharedElementTransitionSource && entry.markForSharedElementTransition

                        val isPotentialSharedElementTransitionTarget =
                                when (entry.popOp) {
                                    TransactionOp.ATTACH, TransactionOp.SHOW -> true
                                    else -> false
                                }

                        val isSharedElementTransitionTarget =
                                isPotentialSharedElementTransitionTarget && entry.markForSharedElementTransition

                        if (
                                isSharedElementTransitionSource &&
                                sourceNamesForSharedElementTransition != null &&
                                targetNamesForSharedElementTransition != null
                        ) {
                            val sourceFragment = manager.findFragmentByTag(entry.tag)
                                    ?: throw IllegalStateException("Cannot find the source fragment for the shared element transition")

                            val namedViewsMap = ArrayMap<String, View>()
                            sourceFragment.view?.findNamedViews(namedViewsMap)

                            overrideTargetEnterTransition = sourceFragment.sharedElementEnterTransition

                            targetNamesForSharedElementTransition
                                    .filter { namedViewsMap.contains(it) }
                                    .map { it to namedViewsMap[it]!! }
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { setSharedElements(it) }
                        } else if (isSharedElementTransitionTarget) {
                            val targetFragment = manager.findFragmentByTag(entry.tag)
                                    ?: throw IllegalStateException("Cannot find the target fragment for the shared element transition")

                            if (targetFragment.sharedElementEnterTransition == null && overrideTargetEnterTransition != null) {
                                targetFragment.sharedElementEnterTransition = overrideTargetEnterTransition
                            }
                        }

                        with(entry) {
                            doPop()
                        }
                    }
        }

        fun popStack(stack: Deque<StackEntry>) {
            (this as TransactionBase)

            while (stack.isNotEmpty()) {
                val top: StackEntry = stack.pop()

                if (top.tag == tag) {
                    if (!inclusive) {
                        stack.push(top)
                    } else {
                        if (!onStackEntryPopped(top)) {
                            top.popOp()
                        }

                        didStackChange = true
                    }

                    break
                } else {
                    if (!onStackEntryPopped(top)) {
                        top.popOp()
                    }

                    didStackChange = true
                }
            }
        }

        popStack(stack)
    }

    /*
    UiStackPersistenceManager
     */
    override fun onSaveStackState(outState: Bundle) {
        outState.putParcelableArrayList(STACK_KEY, ArrayList(stack))
    }

    override fun onRestoreStackState(savedStackState: Bundle) {
        stack.clear()

        val savedStack: List<StackEntry> = savedStackState.getParcelableArrayList(STACK_KEY)
                ?: emptyList()

        stack.addAll(savedStack)
    }

    /*
    OnBackPressListener
     */
    private var backPressHandled = false
    private val backPressListeners = HashSet<OnBackPressListener>()

    override fun onBackPressed(): Boolean {
        backPressHandled = false
        backPressHandled = backPressListeners.fold(initial = false) { wasBackPressed, listener ->
            listener.onBackPressed() || wasBackPressed
        }

        return backPressHandled || when {
            stack.isEmpty() -> false

            else -> {
                commit { pop(stack.peek().tag) }
                true
            }
        }
    }

    /*
    UiStackState
     */
    override val isStackEmpty: Boolean get() = stack.isEmpty()

    override val isStackNotEmpty: Boolean get() = stack.isNotEmpty()

    override fun addOnBackPressListener(listener: OnBackPressListener) {
        backPressListeners += listener
    }

    override fun removeOnBackPressListener(listener: OnBackPressListener) {
        backPressListeners -= listener
    }

    private val stackChangeListeners = HashSet<OnStackChangeListener>()

    override fun addOnStackChangeListener(listener: OnStackChangeListener) {
        stackChangeListeners += listener
    }

    override fun removeOnStackChangeListener(listener: OnStackChangeListener) {
        stackChangeListeners -= listener
    }

    override fun isInStack(tag: String): Boolean =
            stack.any { it.tag == tag }

    override fun isTop(tag: String): Boolean =
            stack.firstOrNull()?.tag == tag
}

private fun List<View>.toTransitionNames() =
        mapNotNullTo(ArrayList(this.size), ViewCompat::getTransitionName).let {
            if (it.isEmpty()) {
                null
            } else {
                it
            }
        }

private fun View.findNamedViews(namedViews: MutableMap<String, View>) {
    if (visibility == View.VISIBLE) {
        val transitionName = ViewCompat.getTransitionName(this)
        if (transitionName != null) {
            namedViews[transitionName] = this
        }
        if (this is ViewGroup) {
            val count = childCount
            (0 until count)
                    .map { getChildAt(it) }
                    .forEach { it.findNamedViews(namedViews) }
        }
    }
}
