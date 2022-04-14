package com.stripe.android.connections.repository

import com.stripe.android.connections.model.LinkAccountSession
import com.stripe.android.connections.model.LinkAccountSessionManifest
import com.stripe.android.connections.model.LinkedAccountList
import com.stripe.android.connections.model.ListLinkedAccountParams
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException

internal interface ConnectionsRepository {
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getLinkedAccounts(
        listLinkedAccountParams: ListLinkedAccountParams
    ): LinkedAccountList

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getLinkAccountSession(
        clientSecret: String
    ): LinkAccountSession

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun generateLinkAccountSessionManifest(
        clientSecret: String,
        applicationId: String
    ): LinkAccountSessionManifest
}
