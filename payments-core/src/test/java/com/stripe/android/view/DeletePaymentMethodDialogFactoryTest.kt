package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CustomerSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DeletePaymentMethodDialogFactoryTest {

    private val customerSession: CustomerSession = mock()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun onDeletedPaymentMethod_shouldCallDetachPaymentMethodAndCallback() {
        var callbackPaymentMethod: PaymentMethod? = null
        val factory = DeletePaymentMethodDialogFactory(
            context,
            mock(),
            CardDisplayTextFactory(context),
            Result.success(customerSession),
            setOf(PaymentMethodsActivity.PRODUCT_TOKEN)
        ) {
            callbackPaymentMethod = it
        }

        factory.onDeletedPaymentMethod(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        verify(customerSession).detachPaymentMethod(
            eq(requireNotNull(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id)),
            eq(setOf(PaymentMethodsActivity.PRODUCT_TOKEN)),
            any()
        )

        assertThat(callbackPaymentMethod)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }
}
