package com.stripe.android.financialconnections.utils

import android.net.Uri
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import javax.inject.Inject

internal class UriUtils @Inject constructor(
    private val logger: Logger,
    private val tracker: FinancialConnectionsAnalyticsTracker,
) {
    suspend fun compareSchemeAuthorityAndPath(
        uriString1: String,
        uriString2: String
    ): Boolean {
        val uri1 = uriString1.toUriOrNull()
        val uri2 = uriString2.toUriOrNull()
        if (uri1 == null || uri2 == null) return false
        return uri1.authority.equals(uri2.authority) &&
            uri1.scheme.equals(uri2.scheme) &&
            uri1.path.equals(uri2.path)
    }

    /**
     * Extracts a query parameter from an URI.
     *
     * stripe-auth://link-accounts/authentication_return?code=failure
     */
    suspend fun getQueryParameter(uri: String, key: String): String? = kotlin.runCatching {
        uri.toUriOrNull()?.getQueryParameter(key)
    }.onFailure { error ->
        tracker.logError(
            "Could not extract query param $key from URI $uri",
            error,
            logger,
            Pane.UNEXPECTED_ERROR
        )
    }.getOrNull()

    /**
     * Extracts a query parameter from the fragment of an URI.
     *
     * stripe-auth://link-accounts/authentication_return#code=failure
     */
    suspend fun getQueryParameterFromFragment(
        uri: String,
        key: String
    ): String? = runCatching {
        uri
            .toUriOrNull()
            ?.fragment
            ?.split("&")
            ?.forEach { param ->
                val keyValue = param.split("=")
                if (keyValue[0] == key && keyValue.size > 1) return keyValue[1]
            }
        return null
    }.onFailure { error ->
        tracker.logError(
            "Could not extract query param $key from URI $uri",
            error,
            logger,
            Pane.UNEXPECTED_ERROR
        )
    }.getOrNull()

    private suspend fun String.toUriOrNull(): Uri? = kotlin.runCatching {
        return Uri.parse(this)
    }.onFailure { error ->
        tracker.logError(
            "Could not parse given URI $this",
            error,
            logger,
            Pane.UNEXPECTED_ERROR
        )
    }.getOrNull()
}
