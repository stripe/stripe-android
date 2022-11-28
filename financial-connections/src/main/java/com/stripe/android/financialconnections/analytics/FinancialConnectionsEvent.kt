package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.ClientPane
import com.stripe.android.financialconnections.utils.filterNotNullValues

/**
 * Event definitions for Financial Connections.
 */
internal sealed class FinancialConnectionsEvent(
    private val name: String,
    val params: Map<String, String>? = null,
    private val includePrefix: Boolean = true
) {

    val eventName = if (includePrefix) "$EVENT_PREFIX.$name" else name

    class PaneLaunched(
        pane: ClientPane,
    ) : FinancialConnectionsEvent(
        "pane.launched",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class PaneLoaded(
        pane: ClientPane,
    ) : FinancialConnectionsEvent(
        "pane.loaded",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickNavBarBack(
        pane: ClientPane,
    ) : FinancialConnectionsEvent(
        name = "click.nav_bar.back",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickNavBarClose(
        pane: ClientPane,
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
        mapOf(
            "num_linked_accounts" to connectedAccounts?.toString(),
            "type" to if (exception == null) "object" else "error"
        )
            .plus(exception?.toEventParams() ?: emptyMap())
            .filterNotNullValues()
    )

    class ClickLearnMoreDataAccess(
        pane: ClientPane
    ) : FinancialConnectionsEvent(
        name = "click.data_access.learn_more",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickDisconnectLink(
        pane: ClientPane
    ) : FinancialConnectionsEvent(
        name = "click.disconnect_link",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Click(
        eventName: String,
        pane: ClientPane
    ) : FinancialConnectionsEvent(
        name = eventName,
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class InstitutionSelected(
        pane: ClientPane,
        fromFeatured: Boolean,
        institutionId: String
    ) : FinancialConnectionsEvent(
        name = if (fromFeatured) "search.featured_institution_selected" else "search.search_result_selected",
        mapOf(
            "pane" to pane.value,
            "institution_id" to institutionId
        ).filterNotNullValues()
    )

    class SearchSucceeded(
        pane: ClientPane,
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
        pane: ClientPane,
    ) : FinancialConnectionsEvent(
        name = "click.link_accounts",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickLinkAnotherAccount(
        pane: ClientPane,
    ) : FinancialConnectionsEvent(
        name = "click.link_another_account",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickDone(
        pane: ClientPane,
    ) : FinancialConnectionsEvent(
        name = "click.done",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Error(
        pane: ClientPane,
        exception: Throwable
    ) : FinancialConnectionsEvent(
        name = if (exception is FinancialConnectionsError) "error.expected" else "error.unexpected",
        params = (
            mapOf("pane" to pane.value)
                .plus(exception.toEventParams())
                .filterNotNullValues()
            )
    )

    class Exposure(
        experimentName: String,
        assignmentEventId: String,
        accountHolderId: String
    ) : FinancialConnectionsEvent(
        name = "preloaded_experiment_retrieved",
        params = mapOf(
            "experiment_retrieved" to experimentName,
            "arb_id" to assignmentEventId,
            "account_holder_id" to accountHolderId
        ).filterNotNullValues(),
        includePrefix = false,
    )

    object ConsentAgree : FinancialConnectionsEvent(
        name = "click.agree",
        mapOf("pane" to ClientPane.CONSENT.value)
    )

    override fun toString(): String {
        return "FinancialConnectionsEvent(name='$name', params=$params)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FinancialConnectionsEvent

        if (name != other.name) return false
        if (params != other.params) return false
        if (includePrefix != other.includePrefix) return false
        if (eventName != other.eventName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (params?.hashCode() ?: 0)
        result = 31 * result + includePrefix.hashCode()
        result = 31 * result + eventName.hashCode()
        return result
    }
}

private const val EVENT_PREFIX = "linked_accounts"
