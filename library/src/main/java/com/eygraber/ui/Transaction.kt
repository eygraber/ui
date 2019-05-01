package com.eygraber.ui

import android.view.View
import androidx.annotation.AnimRes
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

internal enum class TransactionOp {
    ADD,
    ATTACH,
    SHOW,
    HIDE,
    DETACH,
    REMOVE
}

internal fun TransactionOp.reverse() = when (this) {
    TransactionOp.ADD -> TransactionOp.REMOVE
    TransactionOp.ATTACH -> TransactionOp.DETACH
    TransactionOp.SHOW -> TransactionOp.HIDE
    TransactionOp.HIDE -> TransactionOp.SHOW
    TransactionOp.DETACH -> TransactionOp.ATTACH
    TransactionOp.REMOVE -> TransactionOp.ADD
}

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class TransactionDsl

internal abstract class TransactionBase {
    abstract var didStackChange: Boolean
}

interface Transaction {
    fun setSharedElements(vararg sharedElements: Pair<String, View>)
    fun setSharedElements(sharedElements: List<Pair<String, View>>)

    fun add(
            @IdRes id: Int,
            fragment: Fragment,
            tag: String,
            @AnimRes animation: Int? = null,
            markAsTargetFragmentForSharedElementTransition: Boolean = false
    )

    fun addHidden(
            @IdRes id: Int,
            fragment: Fragment,
            tag: String,
            @AnimRes animation: Int? = null
    )

    fun addDetached(
            @IdRes id: Int,
            fragment: Fragment,
            tag: String,
            @AnimRes animation: Int? = null
    )

    fun addDialog(
            fragment: DialogFragment,
            tag: String
    )

    fun attach(
            tag: String,
            @AnimRes animation: Int? = null,
            markAsTargetFragmentForSharedElementTransition: Boolean = false
    )

    fun show(
            tag: String,
            @AnimRes animation: Int? = null,
            markAsTargetFragmentForSharedElementTransition: Boolean = false
    )

    fun hide(
            tag: String,
            @AnimRes animation: Int? = null,
            markAsSourceFragmentForSharedElementTransition: Boolean = false
    )

    fun detach(
            tag: String,
            @AnimRes animation: Int? = null,
            markAsSourceFragmentForSharedElementTransition: Boolean = false
    )

    fun remove(
            tag: String,
            @AnimRes animation: Int? = null,
            markAsSourceFragmentForSharedElementTransition: Boolean = false
    )
}