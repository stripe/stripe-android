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
         * Event when “Agree and continue” is selected on consent pane
         */
        CONSENT_ACQUIRED("consent_acquired"),
        // TODO Add all possible events
    }

    /**
     * Enum representing the error code when an error event is encountered
     */
    enum class ErrorCode(val value: String) {
        INSTITUTION_UNAVAILABLE_PLANNED("institution_unavailable_planned"),
        // TODO Add all possible errors
    }
}
