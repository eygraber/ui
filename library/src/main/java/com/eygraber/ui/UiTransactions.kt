package com.eygraber.ui

import androidx.annotation.MainThread

typealias OnStackEntryPopped = Transaction.(StackEntry) -> Boolean

private val UNHANDLED_ON_STACK_ENTRY_POPPED: OnStackEntryPopped = { false }

@MainThread
interface UiTransactions {
    interface CommitErrorListener {
        fun onError(t: Throwable)
    }

    fun commit(transaction: (@TransactionDsl Transaction).() -> Unit)

    fun clearStack(onStackEntryPopped: OnStackEntryPopped = UNHANDLED_ON_STACK_ENTRY_POPPED)

    fun Transaction.push(tag: String, pushTransaction: (@TransactionDsl PushTransaction).() -> Unit)

    fun Transaction.pop(
            tag: String, inclusive: Boolean = true, onStackEntryPopped: OnStackEntryPopped = UNHANDLED_ON_STACK_ENTRY_POPPED
    )

    fun Transaction.popTop(onStackEntryPopped: OnStackEntryPopped = UNHANDLED_ON_STACK_ENTRY_POPPED)
}