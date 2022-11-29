package com.stripe.android.financialconnections.utils

import android.net.Uri
import com.stripe.android.core.Logger
import javax.inject.Inject

internal class UriUtils @Inject constructor(
    private val logger: Logger
) {
    fun compareSchemeAuthorityAndPath(
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

    fun getQueryParameter(uriString: String, param: String): String? {
        return kotlin.runCatching {
            uriString.toUriOrNull()?.getQueryParameter(param)
        }.onFailure {
            logger.error("Could not extract query param $param from URI $uriString", it)
        }.getOrNull()
    }

    private fun String.toUriOrNull(): Uri? {
        Uri.parse(this).buildUpon().clearQuery()
        return kotlin.runCatching {
            return Uri.parse(this)
        }.onFailure {
            logger.error("Could not parse given URI $this", it)
        }.getOrNull()
    }
}
