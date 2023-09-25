package com.stripe.android.financialconnections.analytics

class FinancialConnectionsEvent internal constructor(
    val name: Name,
    val metadata: Metadata
) {

    /**
     * Metadata for the event
     */
    class Metadata internal constructor(
        val institutionName: String? = null,
        val manualEntry: Boolean? = null,
        val errorCode: ErrorCode? = null
    ) {

        fun toMap(): Map<String, Any?> = mapOf(
            "institution_name" to institutionName,
            "error_code" to errorCode?.value,
            "manual_entry" to manualEntry
        )
    }

    /**
     * Enum representing the name of the event
     */
    enum class Name(val value: String) {
        /**
         * Event when the modal successfully opens
         */
        OPEN("open"),

        /**
         * Event when the modal is launched on an external browser. After receiving this event,
         * no other events will be sent until the browser session is completed with
         * either a [SUCCESS], [CANCEL] or [ERROR].
         */
        FLOW_LAUNCHED_IN_BROWSER("flow_launched_in_browser"),

        /**
         * Event when manual entry flow is initiated
         */
        MANUAL_ENTRY_INITIATED("manual_entry_initiated"),

        /**
         * Event when “Agree and continue” is selected on consent pane
         */
        CONSENT_ACQUIRED("consent_acquired"),

        /**
         * Event when the search bar is selected, the user types some search terms and gets an API response
         */
        SEARCH_INITIATED("search_initiated"),

        /**
         * Event when an institution is selected, either from featured institutions or search results
         */
        INSTITUTION_SELECTED("institution_selected"),

        /**
         * Event when successful authorization is completed
         */
        INSTITUTION_AUTHORIZED("institution_authorized"),

        /**
         * Event when accounts are selected and “confirm” is selected
         */
        ACCOUNTS_SELECTED("accounts_selected"),

        /**
         * Event when the flow is completed and selected accounts are correctly attached to the payment instrument
         */
        SUCCESS("success"),

        /**
         * Event when an error is encountered; see error codes for more
         */
        ERROR("error"),

        /**
         * Event when the modal is closed by the user, either by clicking the "X" or clicking outside the modal
         */
        CANCEL("cancel")
    }

    /**
     * Enum representing the error code when an error event is encountered
     */
    enum class ErrorCode(val value: String) {
        INSTITUTION_UNAVAILABLE_PLANNED("institution_unavailable_planned"),
        INSTITUTION_UNAVAILABLE_UNPLANNED("institution_unavailable_unplanned"),
        INSTITUTION_TIMEOUT("institution_timeout"),
        ACCOUNTS_UNAVAILABLE("accounts_unavailable"),
        ACCOUNT_NUMBERS_UNAVAILABLE("account_numbers_unavailable"),
        WEB_BROWSER_UNAVAILABLE("web_browser_unavailable"),
        NO_DEBITABLE_ACCOUNT("no_debitable_account"),
        AUTHORIZATION_FAILED("authorization_failed"),
        UNEXPECTED_ERROR("unexpected_error"),
        SESSION_EXPIRED("session_expired"),
        FAILED_BOT_DETECTION("failed_bot_detection"),
    }
}
