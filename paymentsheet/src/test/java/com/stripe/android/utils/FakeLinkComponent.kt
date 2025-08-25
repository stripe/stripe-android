package com.stripe.android.utils

import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.FakeLinkAuth
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.attestation.FakeLinkAttestationCheck
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.injection.LinkInlineSignupAssistedViewModelFactory
import com.stripe.android.link.ui.inline.InlineSignupViewModel
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import org.mockito.kotlin.mock

internal class FakeLinkComponent(
    override var configuration: LinkConfiguration = createLinkConfiguration(),
    override val linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
    override val linkGate: LinkGate = FakeLinkGate(),
    override val linkAttestationCheck: LinkAttestationCheck = FakeLinkAttestationCheck(),
    override val linkAuth: LinkAuth = FakeLinkAuth(),
    override val inlineSignupViewModelFactory: LinkInlineSignupAssistedViewModelFactory = object :
        LinkInlineSignupAssistedViewModelFactory {
        override fun create(
            signupMode: LinkSignupMode,
            initialUserInput: UserInput?,
            previousLinkSignupCheckboxSelection: Boolean?
        ): InlineSignupViewModel = mock<InlineSignupViewModel>()
    }
) : LinkComponent() {

    class Builder(private val instance: FakeLinkComponent) : LinkComponent.Builder {
        override fun configuration(configuration: LinkConfiguration): LinkComponent.Builder {
            instance.configuration = configuration
            return this
        }

        override fun build(): LinkComponent {
            return instance
        }
    }
}
