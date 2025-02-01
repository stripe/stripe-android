package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
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
        linkAccountHolder: LinkAccountHolder,
        eventReporter: EventReporter,
        integrityRequestManager: IntegrityRequestManager,
        linkGate: LinkGate,
        errorReporter: ErrorReporter,
        linkAuth: LinkAuth,
        linkConfiguration: LinkConfiguration,
        @Named(START_WITH_VERIFICATION_DIALOG) startWithVerificationDialog: Boolean
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            activityRetainedComponent = component,
            confirmationHandlerFactory = defaultConfirmationHandlerFactory,
            linkAccountManager = linkAccountManager,
            linkAccountHolder = linkAccountHolder,
            eventReporter = eventReporter,
            integrityRequestManager = integrityRequestManager,
            linkGate = linkGate,
            errorReporter = errorReporter,
            linkAuth = linkAuth,
            linkConfiguration = linkConfiguration,
            startWithVerificationDialog = startWithVerificationDialog
        )
    }
}
