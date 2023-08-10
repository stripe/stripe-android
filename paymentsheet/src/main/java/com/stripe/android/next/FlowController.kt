package com.stripe.android.next

import android.app.Activity
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.flow.StateFlow


// Prioritization
// - Customizable
// - Can't get into invalid state
// - Better compose support
// -

internal sealed interface Configurer {
    val initializationMode: PaymentSheet.InitializationMode

    class Intent(val intentConfiguration: PaymentSheet.IntentConfiguration) : Configurer{
        override val initializationMode: PaymentSheet.InitializationMode = PaymentSheet.InitializationMode.DeferredIntent()

    }

    class Payment(val clientSecret: String) : Configurer{

    }
}

class FlowController<C : Configurer> internal constructor(
    val configurer: Configurer,
) {
    // 2 different configurations
    // - Initial setup
    // - Update the current state for things like the intent configuration

    // StateFlow<State>
    // Configure the difference between buy button confirms vs selects

    val state: StateFlow<State> = TODO()

    suspend fun confirm(selected: PaymentMethod): Result<Unit> {
        TODO()
    }

    fun registerActivity(activity: Activity) {

    }

    sealed class State {
        var paymentMethod: PaymentMethod? = null

        class Loading : State()

        class Data(val selected: PaymentMethod?) : State() {
            fun launch() {
                val flowController: FlowController<Configurer.Payment> = TODO()
                flowController.configurer.initializationMode
            }
        }

        class Failure : State() {
            fun retry() {

            }
        }
    }

    companion object {
        fun create(createIntentCallback: CreateIntentCallback): FlowController<Configurer.Intent> {
            return FlowController()
        }

        fun createPaymentIntent(): FlowController<Configurer.Payment> {
            return FlowController()
        }
    }
}

class PaymentSheet(val flowController: FlowController) {
    fun present() {

    }
}

class MerchantViewModel : CreateIntentCallback {
    val flowController = FlowController(this)

    private var clientSecret: String? = null

    init {
        viewModelScope.launch {
            // Do some network call
            // Use the result to store the client secret
            clientSecret = "networkResult"
            flowController.configure()
        }
    }
    override suspend fun onCreateIntent(
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean
    ): CreateIntentResult {
        if (clientSecret != null) {
            return CreateIntentResult.Success(clientSecret)
        } else {
            return CreateIntentResult.Failure(IllegalStateException("hi"))
        }
    }
}

class MerchantActivity {
    val viewModel = MerchantViewModel()

    fun onCreate() {
        var paymentMethod: PaymentMethod? = null
        // Some compose code
        when (viewModel.flowController.state.value) {
            is FlowController.State.Data -> {
                // TODO: Need to consume currently selected and display it
                // TODO: Need or pay with button/icon/view
//                data = it
                // Observe current flow controller state
                val state = FlowController.State.Data(null)
                // Buy button onClick { state.launch(this) }
                state.launch()
            }
            is FlowController.State.Failure -> {
                // Display message, show retry button
                // Add button for retry and call state.retry()
            }
            is FlowController.State.Loading -> {
                // Show loading
            }
        }

        // Some different compose code
//        BuyButton.enabled = paymentMethod != null
    }
}
