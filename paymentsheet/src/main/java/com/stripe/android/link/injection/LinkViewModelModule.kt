package com.stripe.android.link.injection

import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.attestation.IntegrityRequestManager
import dagger.Module
import dagger.Provides

@Module
internal object LinkViewModelModule {
    @Provides
    @NativeLinkScope
    fun provideLinkActivityViewModel(
        component: NativeLinkComponent,
        defaultConfirmationHandlerFactory: DefaultConfirmationHandler.Factory,
        linkAccountManager: LinkAccountManager,
        eventReporter: EventReporter,
        integrityRequestManager: IntegrityRequestManager,
        linkGate: LinkGate
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            activityRetainedComponent = component,
            confirmationHandlerFactory = defaultConfirmationHandlerFactory,
            linkAccountManager = linkAccountManager,
            eventReporter = eventReporter,
            integrityRequestManager = integrityRequestManager,
            linkGate = linkGate
        )
    }
}
