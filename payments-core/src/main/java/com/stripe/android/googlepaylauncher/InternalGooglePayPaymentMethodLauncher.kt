package com.stripe.android.googlepaylauncher

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataCallbackRegistry
import com.stripe.android.googlepaylauncher.callback.GooglePayPaymentDataChangedCallback
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * The internal engine backing [GooglePayPaymentMethodLauncher]. Unlike the public wrapper, this
 * class does not capture the [GooglePayPaymentMethodLauncher.Config], [CardBrandFilter], and
 * [CardFundingFilter] at construction time. Instead they are required unconditionally on [present], which lets
 * callers that only know these values at launch time (such as the confirmation flow) create the launcher up front
 * and supply them later.
 */
@JvmSuppressWildcards
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InternalGooglePayPaymentMethodLauncher @AssistedInject internal constructor(
    @Assisted private val instanceId: String,
    @Assisted private val lifecycleOwner: LifecycleOwner,
    @Assisted onPaymentDataChangedCallback: GooglePayPaymentDataChangedCallback?,
    @Assisted private val activityResultLauncher: ActivityResultLauncher<GooglePayPaymentMethodLauncherContractV2.Args>,
    context: Context,
    paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
        context,
        PaymentConfiguration.getInstance(context).publishableKey,
        setOf(GooglePayPaymentMethodLauncher.PRODUCT_USAGE_TOKEN)
    ),
    analyticsRequestExecutor: AnalyticsRequestExecutor = DefaultAnalyticsRequestExecutor(),
) {
    init {
        if (!GooglePayPaymentMethodLauncher.HAS_SENT_INIT_ANALYTIC_EVENT) {
            GooglePayPaymentMethodLauncher.HAS_SENT_INIT_ANALYTIC_EVENT = true
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(
                    PaymentAnalyticsEvent.GooglePayPaymentMethodLauncherInit
                )
            )
        }

        onPaymentDataChangedCallback?.let {
            GooglePayPaymentDataCallbackRegistry.register(instanceId, it)

            lifecycleOwner.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        GooglePayPaymentDataCallbackRegistry.deregister(instanceId)
                    }
                }
            )
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun present(
        currencyCode: String,
        amount: Long,
        config: GooglePayPaymentMethodLauncher.Config,
        cardBrandFilter: CardBrandFilter,
        cardFundingFilter: CardFundingFilter,
        clientAttributionMetadata: ClientAttributionMetadata?,
        transactionId: String?,
        label: String?,
        isElements: Boolean,
        publishableKey: String?,
        displayItems: List<GooglePayJsonFactory.DisplayItem>,
        billingEmailOverride: String?,
        shippingAddressParameters: GooglePayJsonFactory.ShippingAddressParameters?,
        shippingOptionsParameters: GooglePayJsonFactory.ShippingOptionParameters?,
    ) {
        activityResultLauncher.launch(
            GooglePayPaymentMethodLauncherContractV2.Args(
                dynamicCallbackId = instanceId,
                config = config,
                currencyCode = currencyCode,
                amount = amount,
                label = label,
                transactionId = transactionId,
                cardBrandFilter = cardBrandFilter,
                cardFundingFilter = cardFundingFilter,
                clientAttributionMetadata = clientAttributionMetadata,
                isElements = isElements,
                publishableKey = publishableKey,
                displayItems = displayItems,
                billingEmailOverride = billingEmailOverride,
                shippingAddressParameters = shippingAddressParameters,
                shippingOptionsParameters = shippingOptionsParameters,
            )
        )
    }
}
