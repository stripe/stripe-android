package com.stripe.android.view

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.ShippingInformation
import com.stripe.android.model.ShippingMethod
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.MainScope
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentFlowViewModelTest {

    @Mock
    private lateinit var customerSession: CustomerSession

    private val customerRetrievalListener: KArgumentCaptor<CustomerSession.CustomerRetrievalListener> by lazy {
        argumentCaptor<CustomerSession.CustomerRetrievalListener>()
    }

    private val viewModel: PaymentFlowViewModel by lazy {
        PaymentFlowViewModel(
            customerSession = customerSession,
            paymentSessionData = PaymentSessionData(),
            workScope = MainScope()
        )
    }

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun saveCustomerShippingInformation_onSuccess_returnsExpectedData() {
        val result =
            viewModel.saveCustomerShippingInformation(ShippingInfoFixtures.DEFAULT)
        verify(customerSession).setCustomerShippingInformation(
            eq(ShippingInfoFixtures.DEFAULT),
            customerRetrievalListener.capture()
        )

        customerRetrievalListener.firstValue.onCustomerRetrieved(CustomerFixtures.CUSTOMER)

        val resultValue = requireNotNull(result.value) as PaymentFlowViewModel.SaveCustomerShippingInfoResult.Success
        assertNotNull(resultValue.customer)
    }

    @Test
    fun validateShippingInformation_withValidShippingInfo_createsExpectedShippingMethods() {
        val result = viewModel.validateShippingInformation(
            FakeShippingInformationValidator(),
            FakeShippingMethodsFactory(),
            ShippingInfoFixtures.DEFAULT
        )

        val resultValue =
            requireNotNull(result.value) as PaymentFlowViewModel.ValidateShippingInfoResult.Success
        assertEquals(SHIPPING_METHODS, resultValue.shippingMethods)
    }

    @Test
    fun validateShippingInformation_withValidShippingInfoAndNoShippingMethodsFactory_createsEmptyShippingMethods() {
        val result = viewModel.validateShippingInformation(
            FakeShippingInformationValidator(),
            null,
            ShippingInfoFixtures.DEFAULT
        )

        val resultValue =
            requireNotNull(result.value) as PaymentFlowViewModel.ValidateShippingInfoResult.Success
        assertTrue(resultValue.shippingMethods.isEmpty())
    }

    @Test
    fun validateShippingInformation_withInvalidValidShippingInfo_createsEmptyShippingMethods() {
        val result = viewModel.validateShippingInformation(
            FailingShippingInformationValidator(),
            ThrowingShippingMethodsFactory(),
            ShippingInfoFixtures.DEFAULT
        )

        val resultValue =
            requireNotNull(result.value) as PaymentFlowViewModel.ValidateShippingInfoResult.Error
        assertEquals(SHIPPING_ERROR_MESSAGE, resultValue.errorMessage)
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
            ShippingMethod("UPS Ground", "ups-ground",
                0, "USD", "Arrives in 3-5 days"),
            ShippingMethod("FedEx", "fedex",
                599, "USD", "Arrives tomorrow")
        )
    }
}
