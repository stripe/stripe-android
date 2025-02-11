package com.stripe.android.link.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentsheet.analytics.EventReporter
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
        linkConfiguration: LinkConfiguration,
        linkAttestationCheck: LinkAttestationCheck,
        savedStateHandle: SavedStateHandle,
        @Named(START_WITH_VERIFICATION_DIALOG) startWithVerificationDialog: Boolean
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            activityRetainedComponent = component,
            confirmationHandlerFactory = defaultConfirmationHandlerFactory,
            linkAccountManager = linkAccountManager,
            eventReporter = eventReporter,
            linkConfiguration = linkConfiguration,
            linkAttestationCheck = linkAttestationCheck,
            savedStateHandle = savedStateHandle,
            startWithVerificationDialog = startWithVerificationDialog
        )
    }
}
