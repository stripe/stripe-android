package com.stripe.android.financialconnections.utils

import com.stripe.attestation.IntegrityRequestManager

internal class TestIntegrityRequestManager(
    val prepareResult: Result<Unit> = Result.success(Unit),
    val requestTokenResult: Result<String> = Result.success("token")
) : IntegrityRequestManager {

    override suspend fun prepare(): Result<Unit> = prepareResult

    override suspend fun requestToken(requestIdentifier: String?): Result<String> = requestTokenResult
}
