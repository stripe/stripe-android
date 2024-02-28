package com.stripe.android.financialconnections.utils

import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest

internal class TestHandleError : HandleError {

    private val invocations = mutableListOf<HandleErrorInvocation>()

    override fun invoke(
        extraMessage: String,
        error: Throwable,
        pane: FinancialConnectionsSessionManifest.Pane,
        displayErrorScreen: Boolean
    ) {
        invocations.add(HandleErrorInvocation(extraMessage, error, pane, displayErrorScreen))
    }

    fun assertError(
        extraMessage: String,
        error: Throwable,
        pane: FinancialConnectionsSessionManifest.Pane,
        displayErrorScreen: Boolean
    ) {
        // Check if there is any invocation matching the given parameters
        val match = invocations.any { invocation ->
            invocation.extraMessage == extraMessage &&
                invocation.error == error &&
                invocation.pane == pane &&
                invocation.displayErrorScreen == displayErrorScreen
        }

        // Perform the assertion
        assert(match) {
            "Expected to find an error invocation with " +
                "extraMessage=$extraMessage, " +
                "error=$error, " +
                "pane=$pane, " +
                "displayErrorScreen=$displayErrorScreen, " +
                "but none was found in the invocations list."
        }
    }
}

internal data class HandleErrorInvocation(
    val extraMessage: String,
    val error: Throwable,
    val pane: FinancialConnectionsSessionManifest.Pane,
    val displayErrorScreen: Boolean
)
