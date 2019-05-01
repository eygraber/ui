package com.eygraber.ui

import android.os.Bundle
import androidx.annotation.MainThread

@MainThread
interface UiStackPersistenceManager {
    fun onSaveStackState(outState: Bundle)
    fun onRestoreStackState(savedStackState: Bundle)
}