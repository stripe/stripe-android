package com.stripe.android.crypto.onramp.exception

internal object CryptoOnrampErrorRenderer {
    fun renderGenericApiDeveloperMessage(
        context: APIErrorContext,
        sdkVersions: List<SDKVersion>,
    ): String {
        return renderApiDeveloperMessage(
            context = context,
            summary = context.apiErrorMessage ?: "Stripe API request failed.",
            nextStep = "Inspect the preserved Stripe API error for details and retry after correcting the request.",
            sdkVersions = sdkVersions,
        )
    }

    fun renderApiDeveloperMessage(
        context: APIErrorContext,
        summary: String,
        nextStep: String,
        sdkVersions: List<SDKVersion>,
    ): String {
        return renderDeveloperMessage(
            summary = summary,
            requestContext = requestContextLines(context),
            code = context.apiErrorCode,
            nextStep = nextStep,
            docUrl = context.docUrl,
            sdkVersions = sdkVersions,
        )
    }

    private fun renderDeveloperMessage(
        summary: String,
        requestContext: List<String>,
        code: String?,
        nextStep: String,
        docUrl: String?,
        sdkVersions: List<SDKVersion>,
    ): String {
        val footer = buildList {
            code?.let {
                add("Code: $it")
            }
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

    private fun sdkVersionDescription(sdkVersions: List<SDKVersion>): String {
        val normalizedSdkVersions = sdkVersions.ifEmpty { listOf(SDKVersion.stripeAndroid) }
        return normalizedSdkVersions.joinToString(separator = ", ") { it.debugDescription }
    }

    private fun requestContextLines(
        context: APIErrorContext,
    ): List<String> {
        return listOfNotNull(
            "operation: ${context.operation}",
            "app_id: ${context.appPackageName}",
            context.mode?.let { "mode: $it" },
            context.reason?.let { "reason: $it" },
            context.requestId?.let { "request_id: $it" },
            context.apiErrorType?.let { "type: $it" },
        )
    }
}
