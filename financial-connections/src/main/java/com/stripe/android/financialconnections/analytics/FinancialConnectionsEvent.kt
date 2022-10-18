package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.utils.filterNotNullValues

/**
 * Event definitions for Financial Connections.
 */
internal sealed class FinancialConnectionsEvent(
    val name: String,
    val params: Map<String, String>? = null
) {
    class PaneLaunched(
        pane: NextPane,
    ) : FinancialConnectionsEvent(
        "pane.launched",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class PaneLoaded(
        pane: NextPane,
    ) : FinancialConnectionsEvent(
        "pane.loaded",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickNavBarBack(
        pane: NextPane,
    ) : FinancialConnectionsEvent(
        name = "click.nav_bar.back",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickNavBarClose(
        pane: NextPane,
    ) : FinancialConnectionsEvent(
        name = "click.nav_bar.close",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Complete(
        exception: Throwable?,
        connectedAccounts: Int?
    ) : FinancialConnectionsEvent(
        name = "complete",
        mapOf("num_linked_accounts" to connectedAccounts?.toString())
            .plus(exception?.toEventParams() ?: emptyMap())
            .filterNotNullValues()
    )

    class Click(
        eventName: String,
        pane: NextPane
    ) : FinancialConnectionsEvent(
        name = eventName,
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class InstitutionSelected(pane: NextPane, fromFeatured: Boolean) : FinancialConnectionsEvent(
        name = if (fromFeatured) "search.featured_institution_selected" else "search.search_result_selected",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class SearchSucceeded(
        pane: NextPane,
        query: String,
        duration: Long,
        resultCount: Int
    ) : FinancialConnectionsEvent(
        name = "search.succeeded",
        mapOf(
            "pane" to pane.value,
            "query" to query,
            "duration" to duration.toString(),
            "result_count" to resultCount.toString()
        ).filterNotNullValues()
    )

    class PollAccountsSucceeded(
        authSessionId: String,
        duration: Long
    ) : FinancialConnectionsEvent(
        name = "polling.accounts.success",
        mapOf(
            "authSessionId" to authSessionId,
            "duration" to duration.toString(),
        ).filterNotNullValues()
    )

    class PollAttachPaymentsSucceeded(
        authSessionId: String,
        duration: Long
    ) : FinancialConnectionsEvent(
        name = "polling.attachPayment.success",
        mapOf(
            "authSessionId" to authSessionId,
            "duration" to duration.toString(),
        ).filterNotNullValues()
    )

    class ClickLinkAccounts(
        pane: NextPane,
    ) : FinancialConnectionsEvent(
        name = "click.link_accounts",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickLinkAnotherAccount(
        pane: NextPane,
    ) : FinancialConnectionsEvent(
        name = "click.link_another_account",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickDone(
        pane: NextPane,
    ) : FinancialConnectionsEvent(
        name = "click.done",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Error(
        exception: Throwable
    ) : FinancialConnectionsEvent(
        name = if (exception is FinancialConnectionsError) "error.expected" else "error.unexpected",
        params = exception.toEventParams().filterNotNullValues()
    )

    object ConsentAgree : FinancialConnectionsEvent(
        name = "click.agree",
        mapOf("pane" to NextPane.CONSENT.value)
    )

    override fun toString(): String {
        return "FinancialConnectionsEvent(name='$name', params=$params)"
    }
}

private fun Throwable.toEventParams(): Map<String, String?> = when (this) {
    is FinancialConnectionsError -> mapOf(
        "error" to this.name,
        "error_type" to this.name,
        "error_message" to (stripeError?.message ?: this.message),
        "code" to (stripeError?.code ?: this.statusCode.toString())
    )

    is StripeException -> mapOf(
        "error_type" to (stripeError?.type ?: this::class.java.simpleName),
        "error_message" to (stripeError?.message ?: this.message)?.take(MAX_LOG_LENGTH),
        "code" to (stripeError?.code ?: this.statusCode.toString())
    )

    else -> mapOf(
        "error_type" to this::class.java.simpleName,
        "error_message" to this.message?.take(MAX_LOG_LENGTH),
        "code" to null
    )
}

private const val MAX_LOG_LENGTH = 100
