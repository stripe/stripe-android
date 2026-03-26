package com.stripe.android.googlepaylauncher.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.core.Logger
import com.stripe.android.googlepaylauncher.DefaultGooglePayRepository
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface GooglePayRepositoryFactory {
    operator fun invoke(
        environment: GooglePayEnvironment,
        cardFundingFilter: CardFundingFilter,
        cardBrandFilter: CardBrandFilter
    ): GooglePayRepository
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultGooglePayRepositoryFactory @Inject constructor(
    private val appContext: Context,
    private val logger: Logger,
    private val errorReporter: ErrorReporter,
) : GooglePayRepositoryFactory {
    override fun invoke(
        environment: GooglePayEnvironment,
        cardFundingFilter: CardFundingFilter,
        cardBrandFilter: CardBrandFilter
    ): GooglePayRepository {
        return DefaultGooglePayRepository(
            appContext,
            environment,
            GooglePayJsonFactory.BillingAddressParameters(),
            existingPaymentMethodRequired = true,
            allowCreditCards = true,
            errorReporter = errorReporter,
            logger = logger,
            cardFundingFilter = cardFundingFilter,
            cardBrandFilter = cardBrandFilter
        )
    }
}
