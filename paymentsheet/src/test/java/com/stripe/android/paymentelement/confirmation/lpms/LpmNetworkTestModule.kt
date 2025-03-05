package com.stripe.android.paymentelement.confirmation.lpms

import android.app.Application
import android.content.Context
import com.stripe.android.core.Logger
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal class LpmNetworkTestModule {
    @Provides
    fun providesContext(application: Application): Context = application

    @Provides
    fun providesErrorReporter(): ErrorReporter = FakeErrorReporter()

    @Provides
    @Named(ALLOWS_MANUAL_CONFIRMATION)
    fun providesAllowManualConfirmation(): Boolean = ALLOWS_MANUAL_CONFIRMATION_VALUE

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesProductUsage() = setOf<String>()

    @Provides
    fun providesLogger(): Logger = FakeLogger()

    private companion object {
        const val ALLOWS_MANUAL_CONFIRMATION_VALUE = false
    }
}
