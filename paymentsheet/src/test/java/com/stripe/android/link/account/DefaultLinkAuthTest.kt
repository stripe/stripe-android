package com.stripe.android.link.account

import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.link.TestFactory
import com.stripe.attestation.IntegrityRequestManager
import org.junit.Test

internal class DefaultLinkAuthTest {
    @Test
    fun test() {
        val linkAuth = linkAuth()


    }

    private fun linkAuth(
        useAttestationEndpoints: Boolean = true,
        linkAccountManager: FakeLinkAccountManager = FakeLinkAccountManager(),
        integrityRequestManager: IntegrityRequestManager = FakeIntegrityRequestManager()
    ): DefaultLinkAuth {
        return DefaultLinkAuth(
            linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
                useAttestationEndpointsForLink = useAttestationEndpoints
            ),
            linkAccountManager = linkAccountManager,
            integrityRequestManager = integrityRequestManager,
            applicationId = TestFactory.APP_ID
        )
    }
}