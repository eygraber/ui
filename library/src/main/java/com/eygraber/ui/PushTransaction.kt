package com.eygraber.ui

import android.view.View
import androidx.annotation.AnimRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import java.util.*
import kotlin.collections.ArrayList

internal data class PushTransactionEntry(
        val tag: String,
        @AnimRes val animationId: Int? = null,
        @AnimRes val popAnimationId: Int? = null,
        val op: TransactionOp,
        @IdRes val id: Int? = null,
        val fragment: Fragment? = null,
        val markForSharedElementTransition: Boolean
)

class PushTransaction {
    internal val entries = LinkedList<PushTransactionEntry>()

    internal var sharedElements: ArrayList<Pair<String, View>>? = null

    fun setSharedElements(vararg sharedElements: Pair<String, View>) {
        this.sharedElements = ArrayList(sharedElements.toList())
    }

    fun setSharedElements(sharedElements: List<Pair<String, View>>) {
        this.sharedElements = ArrayList(sharedElements)
    }

    fun add(
            id: Int,
            fragment: Fragment,
            tag: String,
            animationId: Int? = null,
            popAnimationId: Int? = null,
            markAsTargetFragmentForSharedElementTransition: Boolean = false
    ) {
        entries.add(
                PushTransactionEntry(tag, animationId, popAnimationId, TransactionOp.ADD, id, fragment, markForSharedElementTransition = markAsTargetFragmentForSharedElementTransition)
        )
    }

    fun attach(
            tag: String,
            animationId: Int? = null,
            popAnimationId: Int? = null,
            markAsTargetFragmentForSharedElementTransition: Boolean = false
    ) {
        entries.add(
                PushTransactionEntry(tag, animationId, popAnimationId, TransactionOp.ATTACH, markForSharedElementTransition = markAsTargetFragmentForSharedElementTransition)
        )
    }

    fun show(
            tag: String,
            animationId: Int? = null,
            popAnimationId: Int? = null,
            markAsTargetFragmentForSharedElementTransition: Boolean = false
    ) {
        entries.add(
                PushTransactionEntry(tag, animationId, popAnimationId, TransactionOp.SHOW, markForSharedElementTransition = markAsTargetFragmentForSharedElementTransition)
        )
    }

    fun hide(
            tag: String,
            animationId: Int? = null,
            popAnimationId: Int? = null,
            markAsSourceFragmentForSharedElementTransition: Boolean = false
    ) {
        entries.add(
                PushTransactionEntry(tag, animationId, popAnimationId, TransactionOp.HIDE, markForSharedElementTransition = markAsSourceFragmentForSharedElementTransition)
        )
    }

    fun detach(
            tag: String,
            animationId: Int? = null,
            popAnimationId: Int? = null,
            markAsSourceFragmentForSharedElementTransition: Boolean = false
    ) {
        entries.add(
                PushTransactionEntry(tag, animationId, popAnimationId, TransactionOp.DETACH, markForSharedElementTransition = markAsSourceFragmentForSharedElementTransition)
        )
    }
}