package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.core.injection.UIContext
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.validate
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.security.InvalidParameterException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class FlowControllerConfigurationHandler @Inject constructor(
    private val paymentSheetLoader: PaymentSheetLoader,
    @UIContext private val uiContext: CoroutineContext,
    private val eventReporter: EventReporter,
    private val viewModel: FlowControllerViewModel,
    lpmResourceRepository: ResourceRepository<LpmRepository>,
    addressResourceRepository: ResourceRepository<AddressRepository>,
) {
    private val resourceRepositories = listOf(lpmResourceRepository, addressResourceRepository)

    suspend fun configure(
        initializationMode: PaymentSheet.InitializationMode,
        configuration: PaymentSheet.Configuration?,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        try {
            initializationMode.validate()
            configuration?.validate()
        } catch (e: InvalidParameterException) {
            callback.onConfigured(false, e)
            return
        }

        val result = paymentSheetLoader.load(initializationMode, configuration)

        // Wait until all required resources are loaded before completing initialization.
        resourceRepositories.forEach { it.waitUntilLoaded() }

        if (currentCoroutineContext().isActive) {
            viewModel.initializationMode = initializationMode
            dispatchResult(result, callback)
        } else {
            callback.onConfigured(false, null)
        }
    }

    private suspend fun dispatchResult(
        result: PaymentSheetLoader.Result,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) = withContext(uiContext) {
        when (result) {
            is PaymentSheetLoader.Result.Success -> {
                onInitSuccess(result.state, callback)
            }
            is PaymentSheetLoader.Result.Failure -> {
                callback.onConfigured(false, result.throwable)
            }
        }
    }

    private fun onInitSuccess(
        state: PaymentSheetState.Full,
        callback: PaymentSheet.FlowController.ConfigCallback,
    ) {
        eventReporter.onInit(state.config)

        viewModel.paymentSelection = state.initialPaymentSelection
        viewModel.state = state

        callback.onConfigured(true, null)
    }
}
