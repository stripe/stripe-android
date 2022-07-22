package com.stripe.android.financialconnections.repository

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.model.PartnerAccountsList

internal interface FinancialConnectionsRepository {
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getFinancialConnectionsSession(
        clientSecret: String
    ): FinancialConnectionsSession

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postAuthorizationSession(
        clientSecret: String,
        institutionId: String
    ): FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun completeAuthorizationSession(
        clientSecret: String,
        sessionId: String,
        /**
         * MX-specific.
         */
        publicToken: String? = null
    ): FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession

    suspend fun getAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList
}
