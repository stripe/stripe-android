package com.stripe.android.crypto.onramp.exception

internal object CryptoOnrampErrorRenderer {
    fun renderDeveloperMessage(
        summary: String,
        code: String,
        nextStep: String,
        docUrl: String?,
        sdkVersions: List<SDKVersion>,
        requestContext: List<String> = emptyList(),
    ): String {
        val footer = buildList {
            add("Code: $code")
            add("Next step: $nextStep")
            docUrl?.let {
                add("Docs: $it")
            }
            add("SDK: ${sdkVersionDescription(sdkVersions)}")
        }

        return buildList {
            add(summary)
            if (requestContext.isNotEmpty()) {
                add("")
                add("Request Context:")
                addAll(requestContext.map { "  $it" })
            }
            add("")
            addAll(footer)
        }.joinToString(separator = "\n")
    }

    fun requestContextLines(
        diagnosticContext: DiagnosticContext,
        reason: String?,
        requestId: String? = null,
        apiErrorType: String? = null,
    ): List<String> {
        return listOfNotNull(
            "operation: ${diagnosticContext.operation}",
            "app_id: ${diagnosticContext.appPackageName}",
            diagnosticContext.mode?.let { "mode: $it" },
            reason?.let { "reason: $it" },
            requestId?.let { "request_id: $it" },
            apiErrorType?.let { "type: $it" },
        )
    }

    private fun sdkVersionDescription(sdkVersions: List<SDKVersion>): String {
        val normalizedSdkVersions = sdkVersions.ifEmpty { listOf(SDKVersion.stripeAndroid) }
        return normalizedSdkVersions.joinToString(separator = ", ") { it.debugDescription }
    }
}
