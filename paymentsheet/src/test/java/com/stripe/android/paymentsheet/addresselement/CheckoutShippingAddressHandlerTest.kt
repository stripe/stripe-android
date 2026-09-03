@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutControllerReferences
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

internal class CheckoutShippingAddressHandlerTest {
    private val handler = DefaultCheckoutShippingAddressHandler()

    @After
    fun tearDown() {
        CheckoutControllerReferences.clear()
    }

    @Test
    fun `update converts and sends complete address to exact controller`() = runTest {
        val controller = mock<CheckoutController>()
        val unrelatedController = mock<CheckoutController>()
        whenever(controller.updateShippingAddress(anyOrNull(), any())).thenReturn(Result.success(Unit))
        CheckoutControllerReferences.register(CONTROLLER_INSTANCE_ID, controller)
        CheckoutControllerReferences.register("unrelated", unrelatedController)

        val result = handler.update(CONTROLLER_INSTANCE_ID, ADDRESS_DETAILS)

        assertThat(result.isSuccess).isTrue()
        val address = argumentCaptor<CheckoutController.Address>()
        verify(controller).updateShippingAddress(
            name = eq("Jenny Rosen"),
            address = address.capture(),
        )
        assertThat(address.firstValue.build()).isEqualTo(
            CheckoutController.Address.State(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 2",
                postalCode = "94103",
                state = "CA",
            )
        )
        verifyNoInteractions(unrelatedController)
    }

    @Test
    fun `update returns controller failure`() = runTest {
        val controller = mock<CheckoutController>()
        val error = IllegalStateException("Failed")
        whenever(controller.updateShippingAddress(anyOrNull(), any())).thenReturn(Result.failure(error))
        CheckoutControllerReferences.register(CONTROLLER_INSTANCE_ID, controller)

        val result = handler.update(CONTROLLER_INSTANCE_ID, ADDRESS_DETAILS)

        assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
    }

    @Test
    fun `update fails when exact controller is unavailable`() = runTest {
        CheckoutControllerReferences.register("unrelated", mock<CheckoutController>())

        val result = handler.update(CONTROLLER_INSTANCE_ID, ADDRESS_DETAILS)

        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    private companion object {
        const val CONTROLLER_INSTANCE_ID = "CheckoutShippingAddressHandlerTest"

        val ADDRESS_DETAILS = AddressDetails(
            name = "Jenny Rosen",
            address = PaymentSheet.Address(
                city = "San Francisco",
                country = "US",
                line1 = "510 Townsend St",
                line2 = "Floor 2",
                postalCode = "94103",
                state = "CA",
            ),
            phoneNumber = "555-0100",
            isCheckboxSelected = true,
        )
    }
}
