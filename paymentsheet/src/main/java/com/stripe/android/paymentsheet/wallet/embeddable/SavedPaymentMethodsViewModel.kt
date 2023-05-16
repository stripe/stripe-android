package com.stripe.android.paymentsheet.wallet.embeddable

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.ExperimentalPaymentSheetDecouplingApi
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.customer.CustomerAdapterConfig
import com.stripe.android.paymentsheet.customer.PersistablePaymentMethodOption
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.PaymentSheetLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalPaymentSheetDecouplingApi
@Singleton
internal class SavedPaymentMethodsViewModel @Inject constructor(
    val customerAdapterConfig: CustomerAdapterConfig,
    private val customerRepository: CustomerRepository,
//    private val customerAdapter: CustomerAdapter,
    private val paymentSheetLoader: PaymentSheetLoader
) : ViewModel() {

    lateinit var injector: NonFallbackInjector

    private val _paymentMethods =
        MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods

    private val _selectedPaymentMethod = MutableStateFlow<PaymentSelection?>(null)
    val selectedPaymentMethod: StateFlow<PaymentSelection?> = _selectedPaymentMethod

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val backStack = MutableStateFlow<List<SavedPaymentMethodsScreen>>(
        value = listOf(SavedPaymentMethodsScreen.Loading),
    )

    val currentScreen: StateFlow<SavedPaymentMethodsScreen> = backStack
        .map { it.last() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = SavedPaymentMethodsScreen.Loading,
        )

    init {
        viewModelScope.launch {
            paymentSheetLoader.load(
                initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                    PaymentSheet.IntentConfiguration(
                        PaymentSheet.IntentConfiguration.Mode.Setup(
                            null
                        )
                    )
                ),
                paymentSheetConfiguration = PaymentSheet.Configuration(
                    merchantDisplayName = "Test",
                    customer = PaymentSheet.CustomerConfiguration(
                        customerAdapterConfig.customerId,
                        customerAdapterConfig.customerEphemeralKeyProvider()
                    ),
                    billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                        name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
                        address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
                    )
                )
            )
        }

        transitionTo(SavedPaymentMethodsScreen.SelectSavedPaymentMethods)
    }

    fun getPaymentMethods() {
        viewModelScope.launch {
            println("JAMES: $customerAdapterConfig")
            _paymentMethods.value = customerRepository.getPaymentMethods(
                customerConfig = PaymentSheet.CustomerConfiguration(
                    id = customerAdapterConfig.customerId,
                    ephemeralKeySecret = customerAdapterConfig.customerEphemeralKeyProvider()
                ),
                types = listOf(PaymentMethod.Type.Card)
            )
        }
    }

    fun selectPaymentMethod(paymentSelection: PaymentSelection?) {
        viewModelScope.launch {
            when (paymentSelection) {
                is PaymentSelection.GooglePay -> {
                    PersistablePaymentMethodOption.GooglePay
                }
                is PaymentSelection.Link -> {
                    PersistablePaymentMethodOption.Link
                }
                is PaymentSelection.Saved -> {
                    paymentSelection.paymentMethod.id?.let {
                        PersistablePaymentMethodOption.StripeId(it)
                    }
                }
                else -> null
            }?.let { option ->
                _selectedPaymentMethod.update { paymentSelection }
//                customerAdapter.setSelectedPaymentMethodOption(option)
            }
        }
    }

    fun removePaymentMethod(paymentMethod: PaymentMethod) {
        viewModelScope.launch {
            paymentMethod.id?.let {
//                customerAdapter.detachPaymentMethod(it)
            }
        }
    }

    fun transitionToAddCard() {
        transitionTo(SavedPaymentMethodsScreen.AddPaymentMethod)
    }

    private fun transitionTo(target: SavedPaymentMethodsScreen) {
        backStack.update { (it - SavedPaymentMethodsScreen.Loading) + target }
    }

    internal class Factory(
        private val context: Context,
        private val customerAdapterConfig: CustomerAdapterConfig
    ): ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {

        internal data class FallbackInitializeParam(
            val context: Context,
            val customerAdapterConfig: CustomerAdapterConfig
        )
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass == SavedPaymentMethodsViewModel::class.java) {
                val injector = injectWithFallback(
                    "saved_payment_methods_injector",
                    FallbackInitializeParam(context, customerAdapterConfig)
                )

                val viewModel = DaggerSavedPaymentMethodsViewModelFactoryComponent.builder()
                    .context(context)
                    .productUsage(setOf("saved_payment_methods"))
                    .customerAdapterConfig(customerAdapterConfig)
                    .build()
                    .viewModel

                viewModel.injector = requireNotNull(injector as NonFallbackInjector)

                return viewModel as T
            }

            throw IllegalStateException("Unknown ViewModel class: ${modelClass.name}")
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam): Injector {
            val component = DaggerSavedPaymentMethodsViewModelFactoryComponent.builder()
                .context(context)
                .productUsage(setOf("saved_payment_methods"))
                .customerAdapterConfig(customerAdapterConfig)
                .build()
            component.inject(this)
            return component
        }
    }
}