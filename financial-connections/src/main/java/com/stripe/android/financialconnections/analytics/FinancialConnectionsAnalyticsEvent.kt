package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.utils.filterNotNullValues

/**
 * Event definitions for Financial Connections.
 */
internal sealed class FinancialConnectionsAnalyticsEvent(
    private val name: String,
    val params: Map<String, String>? = null,
    private val includePrefix: Boolean = true
) {

    val eventName = if (includePrefix) "$EVENT_PREFIX.$name" else name

    class PaneLaunched(
        pane: Pane,
        referrer: Pane?
    ) : FinancialConnectionsAnalyticsEvent(
        "pane.launched",
        mapOf(
            "referrer_pane" to referrer?.value,
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class AppBackgrounded(
        pane: Pane,
        backgrounded: Boolean,
    ) : FinancialConnectionsAnalyticsEvent(
        if (backgrounded) "mobile.app_entered_background" else "mobile.app_entered_foreground",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class PaneLoaded(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        "pane.loaded",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickNavBarBack(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.nav_bar.back",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickNavBarClose(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.nav_bar.close",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Complete(
        exception: Throwable?,
        exceptionExtraMessage: String?,
        connectedAccounts: Int?
    ) : FinancialConnectionsAnalyticsEvent(
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
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.data_access.learn_more",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class ClickDisconnectLink(
        pane: Pane
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.disconnect_link",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Click(
        eventName: String,
        pane: Pane
    ) : FinancialConnectionsAnalyticsEvent(
        name = eventName,
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class FeaturedInstitutionsLoaded(
        institutionIds: Set<String>,
        duration: Long,
        pane: Pane
    ) : FinancialConnectionsAnalyticsEvent(
        name = "search.feature_institutions_loaded",
        params = (
            institutionIds
                .mapIndexed { index, id -> "institutions[$index]" to id }
                .toMap()
                .plus(
                    mapOf(
                        "pane" to pane.value,
                        "result_count" to institutionIds.size.toString(),
                        "duration" to duration.toString(),
                    )
                )
            ).filterNotNullValues()
    )

    class SearchScroll(
        institutionIds: Set<String>,
        pane: Pane
    ) : FinancialConnectionsAnalyticsEvent(
        name = "search.scroll",
        params = (
            institutionIds
                .mapIndexed { index, id -> "institution_ids[$index]" to id }
                .toMap()
                .plus(
                    mapOf("pane" to pane.value)
                )
            ).filterNotNullValues()
    )

    class InstitutionSelected(
        pane: Pane,
        fromFeatured: Boolean,
        institutionId: String
    ) : FinancialConnectionsAnalyticsEvent(
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
    ) : FinancialConnectionsAnalyticsEvent(
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
    ) : FinancialConnectionsAnalyticsEvent(
        name = "polling.accounts.success",
        mapOf(
            "authSessionId" to authSessionId,
            "duration" to duration.toString(),
        ).filterNotNullValues()
    )

    class AccountSelected(
        selected: Boolean,
        isSingleAccount: Boolean,
        accountId: String,
    ) : FinancialConnectionsAnalyticsEvent(
        name = if (selected) {
            "click.account_picker.account_selected"
        } else {
            "click.account_picker.account_unselected"
        },
        mapOf(
            "is_single_account" to isSingleAccount.toString(),
            "account" to accountId,
        ).filterNotNullValues()
    )

    class PollAttachPaymentsSucceeded(
        authSessionId: String,
        duration: Long
    ) : FinancialConnectionsAnalyticsEvent(
        name = "polling.attachPayment.success",
        mapOf(
            "authSessionId" to authSessionId,
            "duration" to duration.toString(),
        ).filterNotNullValues()
    )

    class ClickLinkAccounts(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.link_accounts",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class NetworkingNewConsumer(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.new_consumer",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class NetworkingReturningConsumer(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.returning_consumer",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class VerificationSuccess(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.verification.success",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class VerificationSuccessNoAccounts(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.verification.success_no_accounts",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class VerificationError(
        pane: Pane,
        error: Error
    ) : FinancialConnectionsAnalyticsEvent(
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
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.verification.step_up.success",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class VerificationStepUpError(
        pane: Pane,
        error: Error
    ) : FinancialConnectionsAnalyticsEvent(
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
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.done",
        mapOf(
            "pane" to pane.value,
        ).filterNotNullValues()
    )

    class Error(
        pane: Pane,
        exception: Throwable,
        extraMessage: String? = null,
    ) : FinancialConnectionsAnalyticsEvent(
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
    ) : FinancialConnectionsAnalyticsEvent(
        name = "preloaded_experiment_retrieved",
        params = mapOf(
            "experiment_retrieved" to experimentName,
            "arb_id" to assignmentEventId,
            "account_holder_id" to accountHolderId
        ).filterNotNullValues(),
        includePrefix = false,
    )

    class AuthSessionUrlReceived(url: String, status: String, authSessionId: String?) :
        FinancialConnectionsAnalyticsEvent(
            name = "auth_session.url_received",
            params = mapOf(
                "status" to status,
                "url" to url,
                "auth_session_id" to (authSessionId ?: "")
            ).filterNotNullValues(),
        )

    class AuthSessionRetrieved(nextPane: Pane, authSessionId: String) :
        FinancialConnectionsAnalyticsEvent(
            name = "auth_session.retrieved",
            params = mapOf(
                "next_pane" to nextPane.value,
                "auth_session_id" to authSessionId,
            ).filterNotNullValues(),
        )

    object ConsentAgree : FinancialConnectionsAnalyticsEvent(
        name = "click.agree",
        mapOf("pane" to Pane.CONSENT.value)
    )

    class PrepaneClickContinue(
        pane: Pane
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.prepane.continue",
        mapOf("pane" to pane.value)
    )

    class AuthSessionOpened(
        pane: Pane,
        flow: String?,
        defaultBrowser: String?,
        id: String
    ) : FinancialConnectionsAnalyticsEvent(
        "auth_session.opened",
        mapOf(
            "auth_session_id" to id,
            "pane" to pane.value,
            "flow" to (flow ?: "unknown"),
            "browser" to (defaultBrowser ?: "unknown")
        ).filterNotNullValues()
    )

    override fun toString(): String {
        return "FinancialConnectionsEvent(name='$name', params=$params)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FinancialConnectionsAnalyticsEvent

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
