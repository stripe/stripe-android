package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.PaymentSessionFixtures
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentFlowViewModelTest {
    private val customerSession: CustomerSession = mock()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val viewModel = PaymentFlowViewModel(
        customerSession = customerSession,
        paymentSessionData = PaymentSessionData(PaymentSessionFixtures.CONFIG),
        workContext = testDispatcher
    )

    @Test
    fun saveCustomerShippingInformation_onSuccess_returnsExpectedData() = runTest {
        whenever(
            customerSession.setCustomerShippingInformation(
                any(),
                any(),
                any()
            )
        ).thenAnswer { invocation ->
            val listener = invocation.arguments[2] as CustomerSession.CustomerRetrievalListener
            listener.onCustomerRetrieved(CustomerFixtures.CUSTOMER)
        }

        assertThat(viewModel.isShippingInfoSubmitted)
            .isFalse()

        val result = viewModel.saveCustomerShippingInformation(ShippingInfoFixtures.DEFAULT)

        verify(customerSession).setCustomerShippingInformation(
            eq(ShippingInfoFixtures.DEFAULT),
            eq(PaymentFlowViewModel.PRODUCT_USAGE),
            any()
        )

        assertThat(result.getOrNull())
            .isNotNull()
        assertThat(viewModel.isShippingInfoSubmitted)
            .isTrue()
    }

    @Test
    fun validateShippingInformation_withValidShippingInfo_createsExpectedShippingMethods() = runTest {
        val result = viewModel.validateShippingInformation(
            FakeShippingInformationValidator(),
            FakeShippingMethodsFactory(),
            ShippingInfoFixtures.DEFAULT
        )

        assertThat(result.getOrNull())
            .isEqualTo(SHIPPING_METHODS)

        assertThat(viewModel.shippingMethods)
            .isEqualTo(SHIPPING_METHODS)
    }

    @Test
    fun validateShippingInformation_withValidShippingInfoAndNoShippingMethodsFactory_createsEmptyShippingMethods() =
        runTest {
            val result = viewModel.validateShippingInformation(
                FakeShippingInformationValidator(),
                null,
                ShippingInfoFixtures.DEFAULT
            )

            assertThat(result.getOrNull())
                .isEmpty()
            assertThat(viewModel.shippingMethods)
                .isEmpty()
        }

    @Test
    fun validateShippingInformation_withInvalidValidShippingInfo_createsEmptyShippingMethods() = runTest {
        val result = viewModel.validateShippingInformation(
            FailingShippingInformationValidator(),
            ThrowingShippingMethodsFactory(),
            ShippingInfoFixtures.DEFAULT
        )

        assertThat(result.exceptionOrNull()?.message)
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
