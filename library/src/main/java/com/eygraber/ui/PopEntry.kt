package com.eygraber.ui

import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.AnimRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PopEntry internal constructor(
    val tag: String,
    @AnimRes internal val popAnimationId: Int?,
    internal val popOp: TransactionOp,
    internal val markForSharedElementTransition: Boolean,
    /* These are used for restoring removed fragments*/
    @IdRes internal val id: Int? = null,
    internal val fqn: String? = null,
    internal val state: Fragment.SavedState? = null,
    internal val args: Bundle? = null
) : Parcelable {
    fun Transaction.doPop() {
        when (popOp) {
            TransactionOp.ATTACH -> attach(tag, popAnimationId, markForSharedElementTransition)
            TransactionOp.SHOW -> show(tag, popAnimationId, markForSharedElementTransition)
            TransactionOp.HIDE -> hide(tag, popAnimationId, markForSharedElementTransition)
            TransactionOp.DETACH -> detach(tag, popAnimationId, markForSharedElementTransition)
            TransactionOp.REMOVE -> remove(tag, popAnimationId, markForSharedElementTransition)

            TransactionOp.ADD -> {
                if(id == null || fqn == null) {
                    throw IllegalStateException(
                        "Cannot restore a removed fragment without an id and fqn"
                    )
                }
                restore(id, tag, popAnimationId, markForSharedElementTransition, fqn, state, args)
            }
        }
    }
}

private fun Transaction.restore(
    @IdRes id: Int,
    tag: String,
    @AnimRes animation: Int? = null,
    markAsTargetFragmentForSharedElementTransition: Boolean = false,
    fqn: String,
    state: Fragment.SavedState?,
    args: Bundle?
) {
    val fragment = Class.forName(fqn).getConstructor().newInstance() as Fragment
    state.let(fragment::setInitialSavedState)
    args.let(fragment::setArguments)

    add(id, fragment, tag, animation, markAsTargetFragmentForSharedElementTransition)
}
