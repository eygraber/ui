package com.eygraber.ui

import android.os.Parcelable
import androidx.annotation.AnimRes
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PopEntry internal constructor(
        val tag: String,
        @AnimRes internal val popAnimationId: Int?,
        internal val popOp: TransactionOp,
        internal val markForSharedElementTransition: Boolean
) : Parcelable {
    fun Transaction.doPop() {
        when (popOp) {
            TransactionOp.ATTACH -> attach(tag, popAnimationId, markForSharedElementTransition)
            TransactionOp.SHOW -> show(tag, popAnimationId, markForSharedElementTransition)
            TransactionOp.HIDE -> hide(tag, popAnimationId, markForSharedElementTransition)
            TransactionOp.DETACH -> detach(tag, popAnimationId, markForSharedElementTransition)
            TransactionOp.REMOVE -> remove(tag, popAnimationId, markForSharedElementTransition)

            // TransactionOp.ADD corresponds to an initial push with a remove which isn't allowed
            else -> throw IllegalStateException("Cannot remove in a GroupedUiTransaction")
        }
    }
}