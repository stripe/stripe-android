package com.stripe.android.link.account

import com.stripe.android.link.TestFactory
import com.stripe.attestation.IntegrityRequestManager
import org.junit.Test

internal class DefaultLinkAuthTest {
    @Test
    fun test() {

    }

    private fun linkAuth(
        useAttestationEndpoints: Boolean,
        linkAccountManager: FakeLinkAccountManager,
        integrityRequestManager: IntegrityRequestManager = FakeIntegr
    ): DefaultLinkAuth {
        return DefaultLinkAuth(
            linkConfiguration = TestFactory.LINK_CONFIGURATION.copy(
                useAttestationEndpointsForLink = useAttestationEndpoints
            ),
            linkAccountManager = linkAccountManager,
            integrityRequestManager =
        )
    }
}