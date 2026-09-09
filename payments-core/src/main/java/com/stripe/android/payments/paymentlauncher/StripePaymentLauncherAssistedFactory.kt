package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.payments.core.injection.INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import javax.inject.Provider

/**
 * [AssistedFactory] to create a [StripePaymentLauncher] with shared dependencies already created
 * elsewhere.
 *
 * Used when [PaymentLauncher] is declared as an internal dependency by another daggerized public
 * API (e.g PaymentSheet).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AssistedFactory
interface StripePaymentLauncherAssistedFactory {
    fun create(
        @Assisted apiConfigurationProvider: Provider<ApiConfiguration.State>,
        @Assisted(STATUS_BAR_COLOR) statusBarColor: Int?,
        @Assisted(INCLUDE_PAYMENT_SHEET_NEXT_ACTION_HANDLERS) includePaymentSheetNextHandlers: Boolean,
        hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
    ): StripePaymentLauncher
}
