package com.eygraber.ui

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class StackEntry internal constructor(
        val tag: String,
        val entries: List<PopEntry>,
        internal val sourceNamesForSharedElementTransition: List<String>?,
        internal val targetNamesForSharedElementTransition: List<String>?
) : Parcelable