package com.stripe.android.paymentsheet.wallet.sheet

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.customer.CustomerAdapter
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerComponent
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerScope
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.launch
import javax.inject.Inject

interface SavedPaymentMethodsSheet {
    suspend fun getPaymentOption(): PaymentOption?

    fun presentSavedPaymentMethods(configuration: Configuration)

    data class Configuration(
        val customerId: String,
        val customerEphemeralKeyProvider: suspend () -> String,
        val merchantDisplayName: String,
        val appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        val googlePayEnabled: Boolean = true,
        val headerTextForSelectionScreen: String? = null,
    )

    companion object {
        fun create(
            activity: ComponentActivity,
            callback: SavedPaymentMethodsSheetResultCallback,
        ) : SavedPaymentMethodsSheet {
            return SavedPaymentMethodsSheetFactory(
                activity = activity,
                callback = callback,
            ).create()
        }
    }
}

@FlowControllerScope
internal class DefaultSavedPaymentMethodsSheet @Inject internal constructor(
    private val lifecycleOwner: LifecycleOwner,
    private val statusBarColor: () -> Int?,
    private val callback: SavedPaymentMethodsSheetResultCallback,
    activityResultCaller: ActivityResultCaller,
    private val customerAdapter: CustomerAdapter
) : SavedPaymentMethodsSheet, NonFallbackInjector {

    /**
     * [FlowControllerComponent] is hold to inject into [Activity]s and created
     * after [DefaultFlowController].
     */
    lateinit var savedPaymentMethodsSheetComponent: SavedPaymentMethodsSheetComponent

    private val savedPaymentMethodsSheetActivityLauncher: ActivityResultLauncher<SavedPaymentMethodsSheetContract.Args> =
        activityResultCaller.registerForActivityResult(
            SavedPaymentMethodsSheetContract()
        ) {
            callback.onResult(it)
        }

    override suspend fun getPaymentOption(): PaymentOption? {
        customerAdapter.fetchSelectedPaymentMethodOption()
        return null
    }

    override fun presentSavedPaymentMethods(configuration: SavedPaymentMethodsSheet.Configuration) {
        customerAdapter.init(
            customerId = configuration.customerId,
            canCreateSetupIntents = true,
            customerEphemeralKeyProvider = configuration.customerEphemeralKeyProvider,
            setupIntentClientSecretProvider = null
        )
        lifecycleOwner.lifecycleScope.launch {
            savedPaymentMethodsSheetActivityLauncher.launch(
                SavedPaymentMethodsSheetContract.Args(
                    statusBarColor = statusBarColor(),
                    injectorKey = "savedPaymentMethodsSheetInjectorKey",
                    productUsage = setOf("savedPaymentMethods"),
                    paymentSheetConfig = PaymentSheet.Configuration(
                        merchantDisplayName = configuration.merchantDisplayName,
                        customer = PaymentSheet.CustomerConfiguration(
                            id = customerAdapter.customerId,
                            ephemeralKeySecret = customerAdapter.customerEphemeralKeyProvider()
                        )
                    )
                )
            )
        }
    }

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is SavedPaymentMethodsSheetViewModel.Factory -> {
                savedPaymentMethodsSheetComponent.stateComponent.inject(injectable)
            }
            is FormViewModel.Factory -> {
                savedPaymentMethodsSheetComponent.stateComponent.inject(injectable)
            }
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }

    companion object {
        fun getInstance(
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            activityResultCaller: ActivityResultCaller,
            statusBarColor: () -> Int?,
            callback: SavedPaymentMethodsSheetResultCallback,
        ): SavedPaymentMethodsSheet{
            val injectorKey =
                WeakMapInjectorRegistry.nextKey(
                    requireNotNull(SavedPaymentMethodsSheet::class.simpleName)
                )

            val savedPaymentMethodsViewModel =
                ViewModelProvider(viewModelStoreOwner)[SavedPaymentMethodsViewModel::class.java]

            val savedPaymentMethodsStateComponent = savedPaymentMethodsViewModel.savedPaymentMethodsSheetStateComponent

            val savedPaymentMethodsSheetComponent: SavedPaymentMethodsSheetComponent =
                savedPaymentMethodsStateComponent.savedPaymentMethodsSheetComponentBuilder
                    .lifeCycleOwner(lifecycleOwner)
                    .activityResultCaller(activityResultCaller)
                    .statusBarColor(statusBarColor)
                    .savedPaymentMethodsSheetResultCallback(callback)
                    .injectorKey(injectorKey)
                    .build()

            val savedPaymentMethodsSheet = savedPaymentMethodsSheetComponent.savedPaymentMethodsSheet
            savedPaymentMethodsSheet.savedPaymentMethodsSheetComponent = savedPaymentMethodsSheetComponent
            WeakMapInjectorRegistry.register(savedPaymentMethodsSheet, injectorKey)
            return savedPaymentMethodsSheet
        }
    }
}