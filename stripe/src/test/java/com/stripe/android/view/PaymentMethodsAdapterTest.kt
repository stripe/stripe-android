package com.stripe.android.view

import androidx.recyclerview.widget.RecyclerView
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.PaymentMethodFixtures
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [PaymentMethodsAdapter]
 */
@RunWith(RobolectricTestRunner::class)
class PaymentMethodsAdapterTest {
    @Mock
    private lateinit var adapterDataObserver: RecyclerView.AdapterDataObserver

    private val paymentMethodsAdapter: PaymentMethodsAdapter =
        PaymentMethodsAdapter(PaymentMethodsActivityStarter.Args.Builder()
            .build()
        )

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        paymentMethodsAdapter.registerAdapterDataObserver(adapterDataObserver)
    }

    @Test
    fun setSelection_changesSelection() {
        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertEquals(4, paymentMethodsAdapter.itemCount)
        verify(adapterDataObserver).onChanged()

        paymentMethodsAdapter.selectedPaymentMethodId = paymentMethodsAdapter.paymentMethods[2].id
        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id,
            requireNotNull(paymentMethodsAdapter.selectedPaymentMethod).id
        )

        paymentMethodsAdapter.selectedPaymentMethodId =
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[1].id
        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[1].id,
            paymentMethodsAdapter.selectedPaymentMethod?.id
        )
    }

    @Test
    fun updatePaymentMethods_removesExistingPaymentMethodsAndAddsAllPaymentMethods() {
        val singlePaymentMethod =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHODS[0])

        paymentMethodsAdapter.setPaymentMethods(singlePaymentMethod)
        paymentMethodsAdapter.selectedPaymentMethodId = paymentMethodsAdapter.paymentMethods[0].id
        assertEquals(2, paymentMethodsAdapter.itemCount)
        assertNotNull(paymentMethodsAdapter.selectedPaymentMethod)

        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[0].id,
            paymentMethodsAdapter.selectedPaymentMethod?.id
        )

        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        paymentMethodsAdapter.selectedPaymentMethodId = paymentMethodsAdapter.paymentMethods[2].id
        assertEquals(4, paymentMethodsAdapter.itemCount)
        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id,
            paymentMethodsAdapter.selectedPaymentMethod?.id
        )
        verify(adapterDataObserver, times(2))
            .onChanged()
    }

    @Test
    fun updatePaymentMethods_withSelection_updatesPaymentMethodsAndSelectionMaintained() {
        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertEquals(4, paymentMethodsAdapter.itemCount)
        paymentMethodsAdapter.selectedPaymentMethodId = PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id
        assertNotNull(paymentMethodsAdapter.selectedPaymentMethod)

        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertEquals(4, paymentMethodsAdapter.itemCount)
        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id,
            paymentMethodsAdapter.selectedPaymentMethod?.id
        )
    }

    @Test
    fun setPaymentMethods_whenNoInitialSpecified_returnsNull() {
        val adapter = PaymentMethodsAdapter(
            intentArgs = PaymentMethodsActivityStarter.Args.Builder()
                .build()
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertNull(adapter.selectedPaymentMethod)
    }

    @Test
    fun setPaymentMethods_whenInitialSpecified_selectsIt() {
        val adapter = PaymentMethodsAdapter(
            intentArgs = PaymentMethodsActivityStarter.Args.Builder()
                .build(),
            initiallySelectedPaymentMethodId = "pm_1000"
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertEquals("pm_1000", adapter.selectedPaymentMethod?.id)
    }
}
