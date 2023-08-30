package com.stripe.android.next

import android.app.Activity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


// Prioritization
// - Customizable
// - Can't get into invalid state
// - Better compose support
// -

internal sealed interface Configurer {
    val initializationMode: PaymentSheet.InitializationMode

    class Intent(val intentConfiguration: PaymentSheet.IntentConfiguration) : Configurer {
        override val initializationMode: PaymentSheet.InitializationMode =
            PaymentSheet.InitializationMode.DeferredIntent()

    }

    class Payment(val clientSecret: String) : Configurer {
        override val initializationMode: PaymentSheet.InitializationMode =
            PaymentSheet.InitializationMode.DeferredIntent()
    }
}

class FlowController internal constructor(
    private val configurer: Configurer,
) {
    // 2 different configurations
    // - Initial setup
    // - Update the current state for things like the intent configuration

    // StateFlow<State>
    // Configure the difference between buy button confirms vs selects

    val state: StateFlow<State> = TODO()

    fun registerActivity(activity: Activity) {

    }

    sealed class State {
        class Loading internal constructor() : State()

        // TODO: Add data for payment method icon/title
        class Data internal constructor(private val selected: PaymentMethod?, val canConfirm: Boolean) : State() {
            fun launch(activity: Activity) {
                val flowController: FlowController = TODO()
                flowController.configurer.initializationMode
            }

            suspend fun confirm(): Result<PaymentMethod> {
                if (!canConfirm) {
                    return
                }
            }
        }

        class Displayed internal constructor()  : State() {

        }

        class Failure internal constructor()  : State() {
            fun retry() {

            }
        }
    }

    companion object {
        fun createDeferredIntent(createIntentCallback: CreateIntentCallback): FlowController {
            return FlowController(Configurer.Intent(createIntentCallback))
        }

        fun createPaymentIntent(clientSecret: String): FlowController {
            return FlowController(Configurer.Payment(clientSecret))
        }
    }
}

class PaymentSheet private constructor(private val flowController: FlowController) {
    val result: Flow<PaymentSheetResult> = flowController.state.map { TODO() }

    fun present(activity: Activity) {
        // Launch a secondary (invisible) activity
        // In the secondary activity view model, we collect the flow controller state
        // When flow controller state is data that is confirmable we call confirm
        // We expose a state from payment sheet
    }

    companion object {
        fun create(flowController: FlowController): PaymentSheet {
            return PaymentSheet(flowController)
        }
    }
}

class MerchantViewModel : ViewModel(), CreateIntentCallback {
    val state: MutableStateFlow<FlowController.State> =
        MutableStateFlow(FlowController.State.Loading()) // TODO: This is internal, how do we do this?

    private var clientSecret: String? = null

    init {
        viewModelScope.launch {
            // Do some network call
            // Use the result to store the client secret
            clientSecret = "networkResult"
            val flowController = FlowController.createDeferredIntent(this@MerchantViewModel)
            flowController.state.collect {
            }
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

class MerchantActivity : AppCompatActivity() {
    val viewModel = MerchantViewModel()

    fun onCreate() {
        setContent {
            val _state by viewModel.state.collectAsState()
            // Some compose code
            when (val state = _state) {
                is FlowController.State.Data -> {
                    // TODO: Need to consume currently selected and display it
                    // TODO: Need or pay with button/icon/view
//                data = it
                    // Observe current flow controller state
                    // Buy button onClick { state.launch(this) }
                    Button(onClick = { state.launch() }) {
                        Text("Show Payment Methods")
                    }
                }

                is FlowController.State.Displayed -> {

                }

                is FlowController.State.Failure -> {
                    Button(onClick = { state.retry() }) {
                        Text("Retry")
                    }
                    // Display message, show retry button
                    // Add button for retry and call state.retry()
                }

                is FlowController.State.Loading -> {
                    Button(onClick = {  }, enabled = false) {
                        Text("Loading")
                    }
                }
            }
        }

        // Some different compose code
//        BuyButton.enabled = paymentMethod != null
    }
}
