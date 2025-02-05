package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.attestation.IntegrityRequestManager
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object LinkViewModelModule {
    @Provides
    @NativeLinkScope
    fun provideLinkActivityViewModel(
        component: NativeLinkComponent,
        defaultConfirmationHandlerFactory: DefaultConfirmationHandler.Factory,
        linkAccountManager: LinkAccountManager,
        eventReporter: EventReporter,
        linkAuth: LinkAuth,
        linkAttestationCheck: LinkAttestationCheck,
        linkConfiguration: LinkConfiguration,
        @Named(START_WITH_VERIFICATION_DIALOG) startWithVerificationDialog: Boolean
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            activityRetainedComponent = component,
            confirmationHandlerFactory = defaultConfirmationHandlerFactory,
            linkAccountManager = linkAccountManager,
            eventReporter = eventReporter,
            linkAuth = linkAuth,
            linkConfiguration = linkConfiguration,
            linkAttestationCheck = linkAttestationCheck,
            startWithVerificationDialog = startWithVerificationDialog
        )
    }
}
