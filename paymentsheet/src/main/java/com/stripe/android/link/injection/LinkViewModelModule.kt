package com.stripe.android.link.injection

import android.app.Application
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityViewModel
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuth
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
        logger: Logger,
        integrityRequestManager: IntegrityRequestManager,
        application: Application,
        linkAuth: LinkAuth
    ): LinkActivityViewModel {
        return LinkActivityViewModel(
            activityRetainedComponent = component,
            confirmationHandlerFactory = defaultConfirmationHandlerFactory,
            linkAccountManager = linkAccountManager,
            eventReporter = eventReporter,
            integrityRequestManager = integrityRequestManager,
            logger = logger,
            applicationId = application.packageName,
            linkAuth = linkAuth,
        )
    }
}
