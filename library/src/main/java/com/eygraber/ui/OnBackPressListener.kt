package com.eygraber.ui

import androidx.annotation.MainThread

@MainThread
interface OnBackPressListener {
    fun onBackPressed(): Boolean
}