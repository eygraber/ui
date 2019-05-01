package com.eygraber.ui

import android.view.View
import androidx.annotation.AnimRes
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

internal open class UiTransaction internal constructor(
        private val transaction: FragmentTransaction,
        private val manager: FragmentManager
) : TransactionBase(), Transaction {
    private var operationPerformed = false

    private var sharedElementTransitionSet = false
    internal var sharedElementTransitionSourceSet = false
    internal var sharedElementTransitionTargetSet = false

    override var didStackChange = false

    override fun setSharedElements(vararg sharedElements: Pair<String, View>) {
        sharedElementTransitionSet = true

        sharedElements.forEach { (name, view) ->
            transaction.addSharedElement(view, name)
        }
    }

    override fun setSharedElements(sharedElements: List<Pair<String, View>>) {
        sharedElementTransitionSet = true

        sharedElements.forEach { (name, view) ->
            transaction.addSharedElement(view, name)
        }
    }

    override fun add(
            @IdRes id: Int,
            fragment: Fragment,
            tag: String,
            @AnimRes animation: Int?,
            markAsTargetFragmentForSharedElementTransition: Boolean
    ): Unit = with(transaction) {
        checkIfTargetFragmentForSharedElementTransitionSet(markAsTargetFragmentForSharedElementTransition)

        if (markAsTargetFragmentForSharedElementTransition) {
            sharedElementTransitionTargetSet = true
        }

        operationPerformed = true

        animation?.let { enterAnimation = it }
        add(id, fragment, tag)
    }

    override fun addHidden(
            @IdRes id: Int,
            fragment: Fragment,
            tag: String,
            @AnimRes animation: Int?
    ): Unit = with(transaction) {
        checkIfTargetFragmentForSharedElementTransitionSet(false)

        operationPerformed = true

        animation?.let { enterAnimation = it }
        add(id, fragment, tag)
        hide(fragment)
    }

    override fun addDetached(
            @IdRes id: Int,
            fragment: Fragment,
            tag: String,
            @AnimRes animation: Int?
    ): Unit = with(transaction) {
        checkIfTargetFragmentForSharedElementTransitionSet(false)

        operationPerformed = true

        animation?.let { enterAnimation = it }
        add(id, fragment, tag)
        detach(fragment)
    }

    override fun addDialog(
            fragment: DialogFragment,
            tag: String
    ) {
        checkIfTargetFragmentForSharedElementTransitionSet(false)

        operationPerformed = true

        transaction.add(fragment, tag)
    }

    override fun attach(
            tag: String,
            @AnimRes animation: Int?,
            markAsTargetFragmentForSharedElementTransition: Boolean
    ): Unit = with(transaction) {
        val fragment = tag.toFragment() ?: if (markAsTargetFragmentForSharedElementTransition) {
            throw IllegalStateException("Couldn't find fragment for tag=$this")
        } else return

        checkIfTargetFragmentForSharedElementTransitionSet(markAsTargetFragmentForSharedElementTransition)

        if (markAsTargetFragmentForSharedElementTransition) {
            sharedElementTransitionTargetSet = true
        }

        operationPerformed = true

        animation?.let { enterAnimation = it }
        attach(fragment)
    }

    override fun show(
            tag: String,
            @AnimRes animation: Int?,
            markAsTargetFragmentForSharedElementTransition: Boolean
    ): Unit = with(transaction) {
        val fragment = tag.toFragment() ?: if (markAsTargetFragmentForSharedElementTransition) {
            throw IllegalStateException("Couldn't find fragment for tag=$this")
        } else return

        checkIfTargetFragmentForSharedElementTransitionSet(markAsTargetFragmentForSharedElementTransition)

        if (markAsTargetFragmentForSharedElementTransition) {
            sharedElementTransitionTargetSet = true
        }

        operationPerformed = true

        animation?.let { enterAnimation = it }
        show(fragment)
    }

    override fun hide(
            tag: String,
            @AnimRes animation: Int?,
            markAsSourceFragmentForSharedElementTransition: Boolean
    ): Unit = with(transaction) {
        val fragment = tag.toFragment() ?: if (markAsSourceFragmentForSharedElementTransition) {
            throw IllegalStateException("Couldn't find fragment for tag=$this")
        } else return

        checkIfSourceFragmentForSharedElementTransitionSet(markAsSourceFragmentForSharedElementTransition)

        if (markAsSourceFragmentForSharedElementTransition) {
            sharedElementTransitionSourceSet = true
        }

        operationPerformed = true

        animation?.let { exitAnimation = it }
        hide(fragment)
    }

    override fun detach(
            tag: String,
            @AnimRes animation: Int?,
            markAsSourceFragmentForSharedElementTransition: Boolean
    ): Unit = with(transaction) {
        val fragment = tag.toFragment() ?: if (markAsSourceFragmentForSharedElementTransition) {
            throw IllegalStateException("Couldn't find fragment for tag=$this")
        } else return

        checkIfSourceFragmentForSharedElementTransitionSet(markAsSourceFragmentForSharedElementTransition)

        if (markAsSourceFragmentForSharedElementTransition) {
            sharedElementTransitionSourceSet = true
        }

        operationPerformed = true

        animation?.let { exitAnimation = it }
        detach(fragment)
    }

    override fun remove(
            tag: String,
            @AnimRes animation: Int?,
            markAsSourceFragmentForSharedElementTransition: Boolean
    ): Unit = with(transaction) {
        val fragment = tag.toFragment() ?: if (markAsSourceFragmentForSharedElementTransition) {
            throw IllegalStateException("Couldn't find fragment for tag=$this")
        } else return

        checkIfSourceFragmentForSharedElementTransitionSet(markAsSourceFragmentForSharedElementTransition)

        if (markAsSourceFragmentForSharedElementTransition) {
            sharedElementTransitionSourceSet = true
        }

        operationPerformed = true

        animation?.let { exitAnimation = it }
        remove(fragment)
    }

    private fun checkIfSourceFragmentForSharedElementTransitionSet(
            markAsSourceFragmentForSharedElementTransition: Boolean
    ) {
        if (markAsSourceFragmentForSharedElementTransition && operationPerformed) {
            throw IllegalStateException("The source fragment for a shared element transition must be first in the transaction")
        } else if (sharedElementTransitionTargetSet) {
            throw IllegalStateException("The target fragment for a shared element transition must be the last in the transaction")
        }
    }

    private fun checkIfTargetFragmentForSharedElementTransitionSet(
            markAsTargetFragmentForSharedElementTransition: Boolean
    ) {
        if (sharedElementTransitionTargetSet) {
            throw IllegalStateException("The target fragment for a shared element transition must be the last in the transaction")
        } else if (markAsTargetFragmentForSharedElementTransition && !sharedElementTransitionSourceSet) {
            throw IllegalStateException("The source fragment for the shared element transition was not set")
        } else if (markAsTargetFragmentForSharedElementTransition && !sharedElementTransitionSet) {
            throw IllegalStateException("The source and target fragments for a shared element transition were set, but the shared elements were not set")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun String.toFragment() =
            manager.findFragmentByTag(this)
}

private inline var FragmentTransaction.enterAnimation: Int
    get() = throw Exception("'enterAnimation' property does not have a getter")
    set(value) {
        setCustomAnimations(value, 0)
    }

private inline var FragmentTransaction.exitAnimation: Int
    get() = throw Exception("'exitAnimation' property does not have a getter")
    set(value) {
        setCustomAnimations(0, value)
    }

private inline var FragmentTransaction.animations: Pair<Int, Int>
    get() = throw Exception("'animations' property does not have a getter")
    set(value) {
        setCustomAnimations(value.first, value.second)
    }