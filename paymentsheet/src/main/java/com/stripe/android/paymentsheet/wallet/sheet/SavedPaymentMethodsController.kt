package com.stripe.android.paymentsheet.wallet.sheet

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.customer.CustomerAdapter
import com.stripe.android.paymentsheet.forms.FormViewModel
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.launch
import javax.inject.Inject

interface SavedPaymentMethodsController {
    suspend fun retrievePaymentOptionSelection(): PaymentOption?
    fun present(configuration: Configuration)

    data class Configuration(
        val merchantDisplayName: String,
        val appearance: PaymentSheet.Appearance = PaymentSheet.Appearance(),
        val googlePayConfiguration: PaymentSheet.GooglePayConfiguration? = null,
        val headerTextForSelectionScreen: String? = null,
    )

    companion object {
        fun create(
            activity: ComponentActivity,
            customerAdapter: CustomerAdapter,
            callback: SavedPaymentMethodsSheetResultCallback,
        ) : SavedPaymentMethodsController {
            return SavedPaymentMethodsControllerFactory(
                activity = activity,
                customerAdapter = customerAdapter,
                callback = callback,
            ).create()
        }

        fun create(
            fragment: Fragment,
            customerAdapter: CustomerAdapter,
            callback: SavedPaymentMethodsSheetResultCallback,
        ) : SavedPaymentMethodsController {
            return SavedPaymentMethodsControllerFactory(
                fragment = fragment,
                customerAdapter = customerAdapter,
                callback = callback,
            ).create()
        }
    }
}

@SavedPaymentMethodsControllerScope
internal class DefaultSavedPaymentMethodsController @Inject internal constructor(
    private val lifecycleOwner: LifecycleOwner,
    private val statusBarColor: () -> Int?,
    private val callback: SavedPaymentMethodsSheetResultCallback,
    private val customerAdapter: CustomerAdapter,
    activityResultCaller: ActivityResultCaller,
) : SavedPaymentMethodsController, NonFallbackInjector {

    /**
     * [SavedPaymentMethodsControllerComponent] is held to inject into [Activity]s and created
     * after [DefaultSavedPaymentMethodsController].
     */
    lateinit var savedPaymentMethodsControllerComponent: SavedPaymentMethodsControllerComponent

    private val savedPaymentMethodsSheetActivityLauncher: ActivityResultLauncher<SavedPaymentMethodsSheetContract.Args> =
        activityResultCaller.registerForActivityResult(
            SavedPaymentMethodsSheetContract()
        ) {
            callback.onResult(it)
        }

    override suspend fun retrievePaymentOptionSelection(): PaymentOption? {
        customerAdapter.fetchSelectedPaymentMethodOption()
        return null
    }

    override fun present(configuration: SavedPaymentMethodsController.Configuration) {
        lifecycleOwner.lifecycleScope.launch {
            savedPaymentMethodsSheetActivityLauncher.launch(
                SavedPaymentMethodsSheetContract.Args(
                    statusBarColor = statusBarColor(),
                    injectorKey = "savedPaymentMethodsSheetInjectorKey",
                    productUsage = setOf("savedPaymentMethods"),
                    paymentSheetConfig = PaymentSheet.Configuration(
                        merchantDisplayName = configuration.merchantDisplayName,
                    )
                )
            )
        }
    }

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is SavedPaymentMethodsSheetViewModel.Factory -> {
                savedPaymentMethodsControllerComponent.stateComponent.inject(injectable)
            }
            is FormViewModel.Factory -> {
                savedPaymentMethodsControllerComponent.stateComponent.inject(injectable)
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
            customerAdapter: CustomerAdapter,
            callback: SavedPaymentMethodsSheetResultCallback,
        ): SavedPaymentMethodsController{
            val injectorKey =
                WeakMapInjectorRegistry.nextKey(
                    requireNotNull(SavedPaymentMethodsController::class.simpleName)
                )

            val savedPaymentMethodsControllerViewModel =
                ViewModelProvider(viewModelStoreOwner)[SavedPaymentMethodsControllerViewModel::class.java]

            val savedPaymentMethodsStateComponent = savedPaymentMethodsControllerViewModel.savedPaymentMethodsControllerStateComponent

            val savedPaymentMethodsControllerComponent: SavedPaymentMethodsControllerComponent =
                savedPaymentMethodsStateComponent.savedPaymentMethodsControllerComponentBuilder
                    .lifeCycleOwner(lifecycleOwner)
                    .activityResultCaller(activityResultCaller)
                    .statusBarColor(statusBarColor)
                    .savedPaymentMethodsSheetResultCallback(callback)
                    .customerAdapter(customerAdapter)
                    .injectorKey(injectorKey)
                    .build()

            val savedPaymentMethodsSheet = savedPaymentMethodsControllerComponent.savedPaymentMethodsSheet
            savedPaymentMethodsSheet.savedPaymentMethodsControllerComponent = savedPaymentMethodsControllerComponent
            WeakMapInjectorRegistry.register(savedPaymentMethodsSheet, injectorKey)
            return savedPaymentMethodsSheet
        }
    }
}