package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal class GooglePayConfirmationModule {
    @JvmSuppressWildcards
    @Provides
    @IntoSet
    fun providesGooglePayConfirmationDefinition(
        googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
        userFacingLogger: UserFacingLogger?,
    ): ConfirmationDefinition<*, *, *, *> {
        return GooglePayConfirmationDefinition(
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
            userFacingLogger = userFacingLogger,
        )
    }
}
