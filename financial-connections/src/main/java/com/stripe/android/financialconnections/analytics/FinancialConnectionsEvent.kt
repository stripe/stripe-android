package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
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
        pane: Pane,
    ) : FinancialConnectionsEvent(
        "pane.launched",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class PaneLoaded(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        "pane.loaded",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickNavBarBack(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "click.nav_bar.back",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickNavBarClose(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "click.nav_bar.close",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Complete(
        exception: Throwable?,
        exceptionExtraMessage: String?,
        connectedAccounts: Int?
    ) : FinancialConnectionsEvent(
        name = "complete",
        mapOf(
            "num_linked_accounts" to connectedAccounts?.toString(),
            "type" to if (exception == null) "object" else "error"
        )
            .plus(exception?.toEventParams(exceptionExtraMessage) ?: emptyMap())
            .filterNotNullValues()
    )

    class ClickLearnMoreDataAccess(
        pane: Pane
    ) : FinancialConnectionsEvent(
        name = "click.data_access.learn_more",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickDisconnectLink(
        pane: Pane
    ) : FinancialConnectionsEvent(
        name = "click.disconnect_link",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Click(
        eventName: String,
        pane: Pane
    ) : FinancialConnectionsEvent(
        name = eventName,
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class InstitutionSelected(
        pane: Pane,
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
        pane: Pane,
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
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "click.link_accounts",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class NetworkingNewConsumer(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "networking.new_consumer",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class NetworkingReturningConsumer(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "networking.returning_consumer",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class VerificationSuccess(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "networking.verification.success",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class VerificationSuccessNoAccounts(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "networking.verification.success_no_accounts",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class VerificationError(
        pane: Pane,
        error: Error
    ) : FinancialConnectionsEvent(
        name = "networking.verification.error",
        mapOf(
            "pane" to pane.value,
            "error" to error.value,
        ).filterNotNullValues()
    ) {
        enum class Error(val value: String) {
            ConsumerNotFoundError("ConsumerNotFoundError"),
            LookupConsumerSession("LookupConsumerSession"),
            StartVerificationSessionError("StartVerificationSessionError"),
            ConfirmVerificationSessionError("ConfirmVerificationSessionError"),
            NetworkedAccountsRetrieveMethodError("NetworkedAccountsRetrieveMethodError"),
        }
    }

    class VerificationStepUpSuccess(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "networking.verification.step_up.success",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class VerificationStepUpError(
        pane: Pane,
        error: Error
    ) : FinancialConnectionsEvent(
        name = "networking.verification.step_up.error",
        mapOf(
            "pane" to pane.value,
            "error" to error.value,
        ).filterNotNullValues()
    ) {
        enum class Error(val value: String) {
            ConsumerNotFoundError("ConsumerNotFoundError"),
            LookupConsumerSession("LookupConsumerSession"),
            StartVerificationError("StartVerificationSessionError"),
            MarkLinkVerifiedError("MarkLinkStepUpAuthenticationVerifiedError"),
        }
    }

    class ClickDone(
        pane: Pane,
    ) : FinancialConnectionsEvent(
        name = "click.done",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Error(
        pane: Pane,
        exception: Throwable,
        extraMessage: String? = null,
    ) : FinancialConnectionsEvent(
        name = when (exception) {
            is FinancialConnectionsError,
            is WebAuthFlowFailedException,
            is OTPError -> "error.expected"

            else -> "error.unexpected"
        },
        params = (
            mapOf("pane" to pane.value)
                .plus(exception.toEventParams(extraMessage))
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

    class AuthSessionCreated(browser: String?, authSessionId: String) : FinancialConnectionsEvent(
        name = "auth_session.created",
        params = mapOf(
            "browser" to browser,
            "auth_session_id" to authSessionId,
        ).filterNotNullValues(),
    )

    class AuthSessionUrlReceived(url: String, status: String, authSessionId: String?) :
        FinancialConnectionsEvent(
            name = "auth_session.url_received",
            params = mapOf(
                "status" to status,
                "url" to url,
                "auth_session_id" to (authSessionId ?: "")
            ).filterNotNullValues(),
        )

    class AuthSessionRetrieved(nextPane: Pane, authSessionId: String) : FinancialConnectionsEvent(
        name = "auth_session.retrieved",
        params = mapOf(
            "next_pane" to nextPane.value,
            "auth_session_id" to authSessionId,
        ).filterNotNullValues(),
    )

    object ConsentAgree : FinancialConnectionsEvent(
        name = "click.agree",
        mapOf("pane" to Pane.CONSENT.value)
    )

    class PrepaneClickContinue(
        pane: Pane
    ) : FinancialConnectionsEvent(
        name = "click.prepane.continue",
        mapOf("pane" to pane.value)
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
