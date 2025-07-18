package com.stripe.android.link.injection

import androidx.lifecycle.SavedStateHandle
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.attestation.LinkAttestationCheck
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.paymentelement.confirmation.DefaultConfirmationHandler
import com.stripe.android.paymentsheet.addresselement.DefaultAutocompleteLauncher
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.uicore.navigation.NavigationManager
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
        linkConfiguration: LinkConfiguration,
        linkAttestationCheck: LinkAttestationCheck,
        linkConfirmationHandlerFactory: LinkConfirmationHandler.Factory,
        navigationManager: NavigationManager,
        savedStateHandle: SavedStateHandle,
        linkLaunchMode: LinkLaunchMode,
        autocompleteLauncher: DefaultAutocompleteLauncher,
        @Named(START_WITH_VERIFICATION_DIALOG) startWithVerificationDialog: Boolean
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            activityRetainedComponent = component,
            confirmationHandlerFactory = defaultConfirmationHandlerFactory,
            linkAccountManager = linkAccountManager,
            linkAccountHolder = linkAccountHolder,
            eventReporter = eventReporter,
            linkConfiguration = linkConfiguration,
            linkAttestationCheck = linkAttestationCheck,
            savedStateHandle = savedStateHandle,
            navigationManager = navigationManager,
            startWithVerificationDialog = startWithVerificationDialog,
            linkConfirmationHandlerFactory = linkConfirmationHandlerFactory,
            linkLaunchMode = linkLaunchMode,
            autocompleteLauncher = autocompleteLauncher,
        )
    }
}
