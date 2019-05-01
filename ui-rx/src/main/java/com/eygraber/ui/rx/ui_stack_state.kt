package com.eygraber.ui.rx

import androidx.annotation.MainThread
import com.eygraber.ui.OnBackPressListener
import com.eygraber.ui.OnStackChangeListener
import com.eygraber.ui.UiStackState
import io.reactivex.Observable

@MainThread
interface BackPressMarker {
    /**
     * This function *MUST* be called on the main thread
     */
    fun markBackPressHandled()
}

fun UiStackState.observeBackPressed(): Observable<BackPressMarker> = Observable.create { emitter ->
    var handled: Boolean
    val marker = object : BackPressMarker {
        override fun markBackPressHandled() {
            handled = true
        }
    }

    var listener: OnBackPressListener? = object : OnBackPressListener {
        override fun onBackPressed(): Boolean {
            handled = false
            emitter.onNext(marker)
            return handled
        }
    }

    addOnBackPressListener(listener!!)

    emitter.setCancellable {
        removeOnBackPressListener(listener!!)
        listener = null
    }
}

fun UiStackState.observeStackChanged(): Observable<Unit> = Observable.create { emitter ->
    var listener: OnStackChangeListener? = object : OnStackChangeListener {
        override fun onStackChanged() {
            emitter.onNext(Unit)
        }
    }

    addOnStackChangeListener(listener!!)

    emitter.setCancellable {
        removeOnStackChangeListener(listener!!)
        listener = null
    }
}