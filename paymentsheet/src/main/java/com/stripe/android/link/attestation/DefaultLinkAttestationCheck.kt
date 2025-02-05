package com.stripe.android.link.attestation

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.model.EmailSource
import com.stripe.attestation.IntegrityRequestManager
import javax.inject.Inject

internal class DefaultLinkAttestationCheck @Inject constructor(
    private val linkGate: LinkGate,
    private val linkAuth: LinkAuth,
    private val integrityRequestManager: IntegrityRequestManager,
    private val linkAccountManager: LinkAccountManager,
    private val linkConfiguration: LinkConfiguration
): LinkAttestationCheck {
    override suspend fun invoke(): LinkAttestationCheck.Result {
        if (linkGate.useAttestationEndpoints.not()) return LinkAttestationCheck.Result.Successful
        integrityRequestManager.prepare()
            .mapCatching {
                val email = linkAccountManager.linkAccount.value?.email
                    ?: linkConfiguration.customerInfo.email
                if (email == null) return@mapCatching LinkAttestationCheck.Result.Successful
                val lookupResult = linkAuth.lookUp(
                    email = email,
                    emailSource = EmailSource.CUSTOMER_OBJECT,
                    startSession = false
                )
                when (lookupResult) {
                    is LinkAuthResult.AttestationFailed -> {
                        LinkAttestationCheck.Result.Failed(lookupResult.throwable)
                    }
                    else -> LinkAttestationCheck.Result.Successful
                }
            }
        TODO("Not yet implemented")
    }
}