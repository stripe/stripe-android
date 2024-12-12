package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals

class SavedPaymentMethodsExtensionTest {

    @Test
    fun `DisplayableSavedPaymentMethod is created correctly when metadata & defaultPaymentMethodId is null`() {
        val context = mock<Context>()
        val resolvableStringValue = "abcde123"
        val resolvableString = mock<ResolvableString>()
        `when`(resolvableString.resolve(context)).thenReturn(resolvableStringValue)
        val providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString = {pmc ->
            resolvableString
        }

        val paymentMethodId = "aaa111"
        val paymentMethod = PaymentMethodFactory.card(paymentMethodId)

        assertEquals(
            expected = DisplayableSavedPaymentMethod.create(
                displayName = resolvableString,
                paymentMethod = paymentMethod,
                isCbcEligible = false,
                shouldShowDefaultBadge = false
            ),
            actual = paymentMethod.toDisplayableSavedPaymentMethod(
                providePaymentMethodName = providePaymentMethodName,
                paymentMethodMetadata = null,
                defaultPaymentMethodId = null
            )
        )
    }

    @Test
    fun `DisplayableSavedPaymentMethod is created correctly when defaultPaymentMethodId not equal to paymentMethod id`() {
        val context = mock<Context>()
        val resolvableStringValue = "abcde123"
        val resolvableString = mock<ResolvableString>()
        `when`(resolvableString.resolve(context)).thenReturn(resolvableStringValue)
        val providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString = {pmc ->
            resolvableString
        }

        val paymentMethodId = "aaa111"
        val paymentMethod = PaymentMethodFactory.card(paymentMethodId)

        val defaultPaymentMethodId = "bbb222"

        assertEquals(
            expected = DisplayableSavedPaymentMethod.create(
                displayName = resolvableString,
                paymentMethod = paymentMethod,
                isCbcEligible = false,
                shouldShowDefaultBadge = false
            ),
            actual = paymentMethod.toDisplayableSavedPaymentMethod(
                providePaymentMethodName = providePaymentMethodName,
                paymentMethodMetadata = null,
                defaultPaymentMethodId = defaultPaymentMethodId
            )
        )
    }

    @Test
    fun `DisplayableSavedPaymentMethod is created correctly when defaultPaymentMethodId equal to paymentMethod id`() {
        val context = mock<Context>()
        val resolvableStringValue = "abcde123"
        val resolvableString = mock<ResolvableString>()
        `when`(resolvableString.resolve(context)).thenReturn(resolvableStringValue)
        val providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString = {pmc ->
            resolvableString
        }

        val paymentMethodId = "aaa111"
        val paymentMethod = PaymentMethodFactory.card(paymentMethodId)

        assertEquals(
            expected = DisplayableSavedPaymentMethod.create(
                displayName = resolvableString,
                paymentMethod = paymentMethod,
                isCbcEligible = false,
                shouldShowDefaultBadge = true
            ),
            actual = paymentMethod.toDisplayableSavedPaymentMethod(
                providePaymentMethodName = providePaymentMethodName,
                paymentMethodMetadata = null,
                defaultPaymentMethodId = paymentMethodId
            )
        )
    }
}
