package com.stripe.android.paymentsheet.wallet.controller

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.customer.CustomerAdapterConfig
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.wallet.sheet.SavedPaymentMethodsSheetContract
import kotlinx.coroutines.launch

interface SavedPaymentMethodsController {
    suspend fun configure(
        merchantDisplayName: String,
        customerAdapterConfig: CustomerAdapterConfig
    )

    fun getPaymentOption(): PaymentOption?

    fun presentSavedPaymentMethods()

    fun presentSavedPaymentMethodsSheet(
        merchantDisplayName: String,
        customerAdapterConfig: CustomerAdapterConfig
    )

    companion object {
        fun create(
            activity: ComponentActivity,
            callback: SavedPaymentMethodsSheetResultCallback
        ): SavedPaymentMethodsController {
            return SavedPaymentMethodsControllerFactory(
                activity = activity,
                callback = callback,
            ).create()
        }

        var customerAdapterConfig: CustomerAdapterConfig = CustomerAdapterConfig(
            customerId = "",
            customerEphemeralKeyProvider = { "" }
        )
    }
}

internal class DefaultSavedPaymentMethodsController(
    private val viewModelStoreOwner: ViewModelStoreOwner,
    private val lifecycleOwner: LifecycleOwner,
    private val activityResultCaller: ActivityResultCaller,
    private val statusBarColor: () -> Int?,
    private val callback: SavedPaymentMethodsSheetResultCallback
) : SavedPaymentMethodsController {

    private val savedPaymentMethodsSheetActivityLauncher: ActivityResultLauncher<SavedPaymentMethodsSheetContract.Args> =
        activityResultCaller.registerForActivityResult(
            SavedPaymentMethodsSheetContract()
        ) {

        }

    private val flowController: PaymentSheet.FlowController

    init {
        flowController = FlowControllerFactory(
            viewModelStoreOwner = viewModelStoreOwner,
            lifecycleOwner = lifecycleOwner,
            activityResultCaller = activityResultCaller,
            statusBarColor = statusBarColor,
            paymentOptionCallback = {
                callback.onResult(
                    SavedPaymentMethodsSheetResult.Success(it)
                )
            },
            paymentResultCallback = {
                // No payment result
            }
        ).create()
    }

    @OptIn(ExperimentalPaymentSheetDecouplingApi::class)
    override suspend fun configure(
        merchantDisplayName: String,
        customerAdapterConfig: CustomerAdapterConfig
    ) {
        flowController.configureWithIntentConfiguration(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(
                    currency = null
                ),
                paymentMethodTypes = listOf(
                    PaymentMethod.Type.Card.code
                )
            ),
            configuration = PaymentSheet.Configuration(
                merchantDisplayName = merchantDisplayName,
                customer = PaymentSheet.CustomerConfiguration(
                    id = customerAdapterConfig.customerId,
                    ephemeralKeySecret = customerAdapterConfig.customerEphemeralKeyProvider()
                )
            ),
            callback = { _, _ -> }
        )
    }

    override fun getPaymentOption(): PaymentOption? {
        return flowController.getPaymentOption()
    }

    override fun presentSavedPaymentMethods() {
        flowController.presentPaymentOptions()
    }

    // This is for the Sheet prototype, the rest is the wrapping prototype
    override fun presentSavedPaymentMethodsSheet(
        merchantDisplayName: String,
        customerAdapterConfig: CustomerAdapterConfig
    ) {
        SavedPaymentMethodsController.customerAdapterConfig = customerAdapterConfig
        lifecycleOwner.lifecycleScope.launch {
            savedPaymentMethodsSheetActivityLauncher.launch(
                SavedPaymentMethodsSheetContract.Args(
                    statusBarColor = statusBarColor(),
                    injectorKey = "savedPaymentMethodsSheetInjectorKey",
                    productUsage = setOf("savedPaymentMethods"),
                    paymentSheetConfig = PaymentSheet.Configuration(
                        merchantDisplayName = merchantDisplayName,
                        customer = PaymentSheet.CustomerConfiguration(
                            id = customerAdapterConfig.customerId,
                            ephemeralKeySecret = customerAdapterConfig.customerEphemeralKeyProvider()
                        )
                    )
                )
            )
        }
    }
}
