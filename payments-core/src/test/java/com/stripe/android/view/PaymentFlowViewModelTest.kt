package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.PaymentSessionFixtures
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class PaymentFlowViewModelTest {
    private val customerSession: CustomerSession = mock()
    private val customerRetrievalListener = argumentCaptor<CustomerSession.CustomerRetrievalListener>()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val viewModel = PaymentFlowViewModel(
        customerSession = customerSession,
        paymentSessionData = PaymentSessionData(PaymentSessionFixtures.CONFIG),
        workContext = testDispatcher
    )

    @Test
    fun saveCustomerShippingInformation_onSuccess_returnsExpectedData() {
        assertThat(viewModel.isShippingInfoSubmitted)
            .isFalse()

        val result =
            viewModel.saveCustomerShippingInformation(ShippingInfoFixtures.DEFAULT)
        verify(customerSession).setCustomerShippingInformation(
            eq(ShippingInfoFixtures.DEFAULT),
            eq(PaymentFlowViewModel.PRODUCT_USAGE),
            customerRetrievalListener.capture()
        )

        var customer: Customer? = null
        result.observeForever {
            customer = it.getOrThrow()
        }
        customerRetrievalListener.firstValue.onCustomerRetrieved(CustomerFixtures.CUSTOMER)

        assertThat(customer)
            .isNotNull()
        assertThat(viewModel.isShippingInfoSubmitted)
            .isTrue()
    }

    @Test
    fun validateShippingInformation_withValidShippingInfo_createsExpectedShippingMethods() {
        val result = viewModel.validateShippingInformation(
            FakeShippingInformationValidator(),
            FakeShippingMethodsFactory(),
            ShippingInfoFixtures.DEFAULT
        )

        var shippingMethods: List<ShippingMethod>? = null
        result.observeForever {
            shippingMethods = it.getOrThrow()
        }
        idleLooper()

        assertThat(shippingMethods)
            .isEqualTo(SHIPPING_METHODS)

        assertThat(viewModel.shippingMethods)
            .isEqualTo(SHIPPING_METHODS)
    }

    @Test
    fun validateShippingInformation_withValidShippingInfoAndNoShippingMethodsFactory_createsEmptyShippingMethods() {
        val result = viewModel.validateShippingInformation(
            FakeShippingInformationValidator(),
            null,
            ShippingInfoFixtures.DEFAULT
        )

        var shippingMethods: List<ShippingMethod>? = null
        result.observeForever {
            shippingMethods = it.getOrThrow()
        }
        idleLooper()

        assertThat(shippingMethods)
            .isEmpty()
        assertThat(viewModel.shippingMethods)
            .isEmpty()
    }

    @Test
    fun validateShippingInformation_withInvalidValidShippingInfo_createsEmptyShippingMethods() {
        val result = viewModel.validateShippingInformation(
            FailingShippingInformationValidator(),
            ThrowingShippingMethodsFactory(),
            ShippingInfoFixtures.DEFAULT
        )

        var throwable: Throwable? = null
        result.observeForever {
            throwable = it.exceptionOrNull()
        }
        idleLooper()

        assertThat(throwable?.message)
            .isEqualTo(SHIPPING_ERROR_MESSAGE)
    }

    private class FakeShippingInformationValidator : PaymentSessionConfig.ShippingInformationValidator {
        override fun isValid(shippingInformation: ShippingInformation): Boolean {
            return true
        }

        override fun getErrorMessage(shippingInformation: ShippingInformation): String {
            throw IllegalStateException("Should not be called because shipping info is valid")
        }
    }

    private class FakeShippingMethodsFactory : PaymentSessionConfig.ShippingMethodsFactory {
        override fun create(shippingInformation: ShippingInformation): List<ShippingMethod> {
            return SHIPPING_METHODS
        }
    }

    private class FailingShippingInformationValidator : PaymentSessionConfig.ShippingInformationValidator {
        override fun isValid(shippingInformation: ShippingInformation): Boolean {
            return false
        }

        override fun getErrorMessage(shippingInformation: ShippingInformation): String {
            return SHIPPING_ERROR_MESSAGE
        }
    }

    private class ThrowingShippingMethodsFactory : PaymentSessionConfig.ShippingMethodsFactory {
        override fun create(shippingInformation: ShippingInformation): List<ShippingMethod> {
            throw RuntimeException("Always throws an exception")
        }
    }

    private companion object {
        private const val SHIPPING_ERROR_MESSAGE = "Shipping info was invalid"
        private val SHIPPING_METHODS = listOf(
            ShippingMethod(
                "UPS Ground",
                "ups-ground",
                0,
                "USD",
                "Arrives in 3-5 days"
            ),
            ShippingMethod(
                "FedEx",
                "fedex",
                599,
                "USD",
                "Arrives tomorrow"
            )
        )
    }
}
