package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerAdapterDataRepository(
    @IOContext private val workContext: CoroutineContext,
    private val adapter: CustomerAdapter,
    private val elementsSessionRepository: ElementsSessionRepository,
) : CustomerSheetDataRepository {
    override val canCreateSetupIntents: Boolean = adapter.canCreateSetupIntents

    override val paymentMethodTypes: List<String> = adapter.paymentMethodTypes ?: emptyList()

    override suspend fun retrievePaymentMethods(): Result<List<PaymentMethod>> {
        return adapter.retrievePaymentMethods().toResult()
    }

    override suspend fun loadCustomerSheetSession(): Result<CustomerSheetSession> = withContext(workContext) {
        runCatching {
            val elementsSession = async {
                retrieveElementsSession()
            }

            val paymentMethods = async {
                retrievePaymentMethods()
            }

            CustomerSheetSession(
                elementsSession = elementsSession.await().getOrThrow(),
                paymentMethods = paymentMethods.await().getOrThrow(),
            )
        }
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String) = withContext(workContext) {
        adapter.attachPaymentMethod(paymentMethodId).toResult()
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ) = withContext(workContext) {
        adapter.updatePaymentMethod(paymentMethodId, params).toResult()
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String) = withContext(workContext) {
        adapter.detachPaymentMethod(paymentMethodId).toResult()
    }

    override suspend fun retrieveSelectedPaymentOption() = withContext(workContext) {
        adapter.retrieveSelectedPaymentOption().toResult().map { option ->
            when (option) {
                is CustomerAdapter.PaymentOption.GooglePay -> CustomerPaymentOption.GooglePay
                is CustomerAdapter.PaymentOption.Link -> CustomerPaymentOption.Link
                is CustomerAdapter.PaymentOption.StripeId -> CustomerPaymentOption.Saved(option.id)
                null -> CustomerPaymentOption.None
            }
        }
    }

    override suspend fun setSelectedPaymentOption(paymentOption: CustomerPaymentOption) = withContext(workContext) {
        val adapterOption = when (paymentOption) {
            is CustomerPaymentOption.GooglePay -> CustomerAdapter.PaymentOption.GooglePay
            is CustomerPaymentOption.Link -> CustomerAdapter.PaymentOption.Link
            is CustomerPaymentOption.Saved -> CustomerAdapter.PaymentOption.StripeId(paymentOption.id)
            is CustomerPaymentOption.None -> null
        }

        adapter.setSelectedPaymentOption(adapterOption).toResult()
    }

    override suspend fun retrieveSetupIntentClientSecret() = withContext(workContext) {
        adapter.setupIntentClientSecretForCustomerAttach().toResult()
    }

    private suspend fun retrieveElementsSession(): Result<ElementsSession> {
        val paymentMethodTypes = if (adapter.canCreateSetupIntents) {
            adapter.paymentMethodTypes ?: emptyList()
        } else {
            // We only support cards if `canCreateSetupIntents` is false.
            listOf("card")
        }

        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                paymentMethodTypes = paymentMethodTypes,
            )
        )

        return elementsSessionRepository.get(
            initializationMode = initializationMode,
            customer = null,
            externalPaymentMethods = listOf()
        )
    }

    private fun <T> CustomerAdapter.Result<T>.toResult(): Result<T> {
        return when (this) {
            is CustomerAdapter.Result.Success -> Result.success(value)
            is CustomerAdapter.Result.Failure -> Result.failure(cause)
        }
    }
}
