package com.stripe.android.utils

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.MobileCardElementConfig
import com.stripe.android.testing.AbsFakeStripeRepository
import com.stripe.android.view.CardWidgetViewModel

internal object CardElementTestHelper {

    fun createViewModelStoreOwner(
        isCbcEligible: Boolean,
    ): ViewModelStoreOwner {
        val cardWidgetViewModel = CardWidgetViewModel(
            paymentConfigProvider = {
                PaymentConfiguration(
                    publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
                    stripeAccountId = null,
                )
            },
            stripeRepository = object : AbsFakeStripeRepository() {
                override suspend fun retrieveCardElementConfig(
                    requestOptions: ApiRequest.Options,
                ): Result<MobileCardElementConfig> {
                    return Result.success(
                        MobileCardElementConfig(
                            cardBrandChoice = MobileCardElementConfig.CardBrandChoice(
                                eligible = isCbcEligible,
                            ),
                        )
                    )
                }
            },
        )

        val viewModelStore = ViewModelStore().apply {
            val className = CardWidgetViewModel::class.java.canonicalName
            put("androidx.lifecycle.ViewModelProvider.DefaultKey:$className", cardWidgetViewModel)
        }

        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = viewModelStore
        }
    }
}
