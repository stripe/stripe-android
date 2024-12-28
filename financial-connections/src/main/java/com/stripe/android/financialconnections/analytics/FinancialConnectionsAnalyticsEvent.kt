package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.ConsentAgree.analyticsValue
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.utils.filterNotNullValues
import com.stripe.attestation.AttestationError

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
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class AppBackgrounded(
        pane: Pane,
        backgrounded: Boolean,
    ) : FinancialConnectionsAnalyticsEvent(
        if (backgrounded) "mobile.app_entered_background" else "mobile.app_entered_foreground",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class PaneLoaded(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        "pane.loaded",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class ClickNavBarBack(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.nav_bar.back",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class ClickNavBarClose(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.nav_bar.close",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class Complete(
        pane: Pane,
        exception: Throwable?,
        exceptionExtraMessage: String?,
        connectedAccounts: Int?,
        status: String,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "complete",
        mapOf(
            "pane" to pane.analyticsValue,
            "num_linked_accounts" to connectedAccounts?.toString(),
            "type" to if (exception == null) "object" else "error",
            "status" to status,
        )
            .plus(exception?.toEventParams(exceptionExtraMessage) ?: emptyMap())
            .filterNotNullValues()
    )

    class ClickLearnMoreDataAccess(
        pane: Pane
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.data_access.learn_more",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class Click(
        eventName: String,
        pane: Pane
    ) : FinancialConnectionsAnalyticsEvent(
        name = eventName,
        mapOf(
            "pane" to pane.analyticsValue,
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
                        "pane" to pane.analyticsValue,
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
                    mapOf("pane" to pane.analyticsValue)
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
            "pane" to pane.analyticsValue,
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
            "pane" to pane.analyticsValue,
            "query" to query,
            "duration" to duration.toString(),
            "result_count" to resultCount.toString()
        ).filterNotNullValues()
    )

    class PollAccountsSucceeded(
        pane: Pane,
        authSessionId: String,
        duration: Long
    ) : FinancialConnectionsAnalyticsEvent(
        name = "polling.accounts.success",
        mapOf(
            "pane" to pane.analyticsValue,
            "authSessionId" to authSessionId,
            "duration" to duration.toString(),
        ).filterNotNullValues()
    )

    class AccountSelected(
        pane: Pane,
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
            "pane" to pane.analyticsValue,
            "is_single_account" to isSingleAccount.toString(),
            "account" to accountId,
        ).filterNotNullValues()
    )

    class AccountsSubmitted(
        pane: Pane,
        accountIds: Set<String>,
        isSkipAccountSelection: Boolean
    ) : FinancialConnectionsAnalyticsEvent(
        name = "account_picker.accounts_submitted",
        mapOf(
            "pane" to pane.analyticsValue,
            "account_ids" to accountIds.joinToString(" "),
            "is_skip_account_selection" to isSkipAccountSelection.toString(),
        ).filterNotNullValues()
    )

    class AccountsAutoSelected(
        pane: Pane,
        accountIds: Set<String>,
        isSingleAccount: Boolean
    ) : FinancialConnectionsAnalyticsEvent(
        name = "account_picker.accounts_auto_selected",
        mapOf(
            "pane" to pane.analyticsValue,
            "account_ids" to accountIds.joinToString(" "),
            "is_single_account" to isSingleAccount.toString(),
        ).filterNotNullValues()
    )

    class PollAttachPaymentsSucceeded(
        pane: Pane,
        authSessionId: String,
        duration: Long
    ) : FinancialConnectionsAnalyticsEvent(
        name = "polling.attachPayment.success",
        mapOf(
            "pane" to pane.analyticsValue,
            "authSessionId" to authSessionId,
            "duration" to duration.toString(),
        ).filterNotNullValues()
    )

    class ClickLinkAccounts(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.link_accounts",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class NetworkingNewConsumer(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.new_consumer",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class NetworkingReturningConsumer(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.returning_consumer",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class VerificationSuccess(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.verification.success",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class VerificationError(
        pane: Pane,
        error: Error
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.verification.error",
        mapOf(
            "pane" to pane.analyticsValue,
            "error" to error.value,
        ).filterNotNullValues()
    ) {
        enum class Error(val value: String) {
            ConsumerNotFoundError("ConsumerNotFoundError"),
            LookupConsumerSession("LookupConsumerSession"),
            StartVerificationSessionError("StartVerificationSessionError"),
            ConfirmVerificationSessionError("ConfirmVerificationSessionError"),
            MarkLinkVerifiedError("MarkLinkVerifiedError"),
        }
    }

    class VerificationStepUpSuccess(
        pane: Pane,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.verification.step_up.success",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class VerificationStepUpError(
        pane: Pane,
        error: Error
    ) : FinancialConnectionsAnalyticsEvent(
        name = "networking.verification.step_up.error",
        mapOf(
            "pane" to pane.analyticsValue,
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
            "pane" to pane.analyticsValue,
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
            mapOf("pane" to pane.analyticsValue)
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

    class AuthSessionUrlReceived(
        pane: Pane,
        url: String,
        status: String,
        authSessionId: String?
    ) : FinancialConnectionsAnalyticsEvent(
        name = "auth_session.url_received",
        params = mapOf(
            "pane" to pane.analyticsValue,
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

    data object ConsentAgree : FinancialConnectionsAnalyticsEvent(
        name = "click.agree",
        mapOf("pane" to Pane.CONSENT.value)
    )

    class PrepaneClickContinue(
        pane: Pane
    ) : FinancialConnectionsAnalyticsEvent(
        name = "click.prepane.continue",
        mapOf("pane" to pane.analyticsValue)
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
            "pane" to pane.analyticsValue,
            "flow" to (flow ?: "unknown"),
            "browser" to (defaultBrowser ?: "unknown")
        ).filterNotNullValues()
    )

    class AttestationInitSkipped(pane: Pane) : FinancialConnectionsAnalyticsEvent(
        name = "attestation.init_skipped",
        mapOf(
            "pane" to pane.analyticsValue,
        ).filterNotNullValues()
    )

    class AttestationInitFailed(
        pane: Pane,
        error: Throwable
    ) : FinancialConnectionsAnalyticsEvent(
        name = "attestation.init_failed",
        mapOf(
            "pane" to pane.analyticsValue,
            "error_reason" to if (error is AttestationError) error.errorType.name else "unknown"
        ).filterNotNullValues()
    )

    class AttestationRequestSucceeded(
        pane: Pane,
        endpoint: AttestationEndpoint
    ) : FinancialConnectionsAnalyticsEvent(
        name = "attestation.request_token_succeeded",
        mapOf(
            "pane" to pane.analyticsValue,
            "api" to endpoint.analyticsValue
        ).filterNotNullValues()
    )

    class AttestationRequestFailed(
        pane: Pane,
        endpoint: AttestationEndpoint,
        error: Throwable,
    ) : FinancialConnectionsAnalyticsEvent(
        name = "attestation.request_token_failed",
        mapOf(
            "pane" to pane.analyticsValue,
            "api" to endpoint.analyticsValue,
            "error_reason" to if (error is AttestationError) error.errorType.name else "unknown"
        )
    )

    internal val Pane.analyticsValue
        get() = when (this) {
            // We want to log partner_auth regardless of the pane being shown full-screen or as a drawer.
            Pane.PARTNER_AUTH_DRAWER,
            Pane.PARTNER_AUTH -> Pane.PARTNER_AUTH.value
            else -> value
        }

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

    enum class AttestationEndpoint(val analyticsValue: String) {
        LOOKUP("consumer_session_lookup"),
        SIGNUP("link_sign_up"),
    }
}

private const val EVENT_PREFIX = "linked_accounts"
