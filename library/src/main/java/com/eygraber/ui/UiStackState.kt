package com.eygraber.ui

import androidx.annotation.MainThread

@MainThread
interface UiStackState {
    val isStackEmpty: Boolean
    val isStackNotEmpty: Boolean

    @MainThread
    interface BackPressMarker {
        /**
         * This function *MUST* be called on the main thread
         */
        fun markBackPressHandled()
    }

    fun addOnBackPressListener(listener: OnBackPressListener)
    fun removeOnBackPressListener(listener: OnBackPressListener)

    fun addOnStackChangeListener(listener: OnStackChangeListener)
    fun removeOnStackChangeListener(listener: OnStackChangeListener)

    fun isInStack(tag: String): Boolean

    fun isTop(tag: String): Boolean
}