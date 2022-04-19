package com.stripe.android.financialconnections.repository

import com.stripe.android.financialconnections.model.LinkAccountSession
import com.stripe.android.financialconnections.model.LinkAccountSessionManifest
import com.stripe.android.financialconnections.model.LinkedAccountList
import com.stripe.android.financialconnections.model.ListLinkedAccountParams
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException

internal interface FinancialConnectionsRepository {
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
