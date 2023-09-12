package com.stripe.android.financialconnections.analytics

sealed class FinancialConnectionsEvent(val name: String) {

    /**
     * Event when the modal successfully opens
     */
    object Open : FinancialConnectionsEvent("open")

    /**
     * Event when manual entry flow is initiated
     */
    object ManualEntryInitiated : FinancialConnectionsEvent("manual_entry_initiated")

    /**
     * Event when bank account data is manually entered and “continue” is successfully clicked
     */
    object MicrodepositsInitiated : FinancialConnectionsEvent("microdeposits_initiated")

    /**
     * Event when “Agree and continue” is selected on consent pane
     */
    object ConsentAcquired : FinancialConnectionsEvent("consent_acquired")

    /**
     * Event when the search bar is selected, the user types some search terms and gets an API response
     */
    object SearchInitiated : FinancialConnectionsEvent("search_initiated")

    /**
     * Event when an institution is selected, either from featured institutions or search results
     */
    data class InstitutionSelected(val institutionName: String) : FinancialConnectionsEvent("institution_selected")

    /**
     * Event when successful authorization is completed
     */
    object InstitutionAuthorized : FinancialConnectionsEvent("institution_authorized")

    /**
     * Event when accounts are selected and “confirm” is selected
     */
    object AccountsSelected : FinancialConnectionsEvent("accounts_selected")

    /**
     * Event when the flow is completed and selected accounts are correctly attached to the payment instrument
     */
    data class Success(val manualEntry: Boolean? = null) : FinancialConnectionsEvent("success")

    /**
     * Event when an error is encountered; see error codes for more
     */
    data class Error(val errorCode: ErrorCode) : FinancialConnectionsEvent("error") {
        enum class ErrorCode {
            INSTITUTION_UNAVAILABLE_PLANNED,
            INSTITUTION_UNAVAILABLE_UNPLANNED,
            INSTITUTION_TIMEOUT,
            ACCOUNTS_UNAVAILABLE,
            NO_DEBITABLE_ACCOUNT,
            AUTHORIZATION_FAILED,
            UNEXPECTED_ERROR,
            SESSION_EXPIRED,
            FAILED_BOT_DETECTION
        }
    }

    /**
     * Event when the modal is closed by the user, either by clicking the "X" or clicking outside the modal
     */
    object Cancel : FinancialConnectionsEvent("cancel")
}
