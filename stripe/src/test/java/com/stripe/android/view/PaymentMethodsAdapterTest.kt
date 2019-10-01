package com.stripe.android.view

import androidx.recyclerview.widget.RecyclerView
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import java.util.Objects
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [PaymentMethodsAdapter]
 */
@RunWith(RobolectricTestRunner::class)
class PaymentMethodsAdapterTest {
    @Mock
    private lateinit var adapterDataObserver: RecyclerView.AdapterDataObserver
    private lateinit var paymentMethodsAdapter: PaymentMethodsAdapter

    @BeforeTest
    fun setup() {
        PaymentMethodFixtures.createCard()
        MockitoAnnotations.initMocks(this)
        paymentMethodsAdapter = PaymentMethodsAdapter(null,
            PaymentMethodsActivityStarter.Args.Builder()
                .build())
        paymentMethodsAdapter.registerAdapterDataObserver(adapterDataObserver)
    }

    @Test
    fun setSelection_changesSelection() {
        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertEquals(4, paymentMethodsAdapter.itemCount)
        verify<RecyclerView.AdapterDataObserver>(adapterDataObserver).onChanged()

        assertEquals(PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id,
            Objects.requireNonNull<PaymentMethod>(paymentMethodsAdapter.selectedPaymentMethod).id)

        paymentMethodsAdapter.selectedPaymentMethodId = PaymentMethodFixtures.CARD_PAYMENT_METHODS[1].id
        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[1].id,
            paymentMethodsAdapter.selectedPaymentMethod?.id
        )
    }

    @Test
    fun updatePaymentMethods_removesExistingPaymentMethodsAndAddsAllPaymentMethods() {
        val singlePaymentMethod = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHODS[0])

        paymentMethodsAdapter.setPaymentMethods(singlePaymentMethod)
        assertEquals(2, paymentMethodsAdapter.itemCount)
        assertNotNull(paymentMethodsAdapter.selectedPaymentMethod)

        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[0].id,
            paymentMethodsAdapter.selectedPaymentMethod?.id
        )

        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertEquals(4, paymentMethodsAdapter.itemCount)
        assertEquals(
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id,
            paymentMethodsAdapter.selectedPaymentMethod?.id
        )
        verify<RecyclerView.AdapterDataObserver>(adapterDataObserver, times(2))
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
    fun setPaymentMethods_whenNoInitialSpecified_selectsMostRecentlyCreated() {
        val adapter = PaymentMethodsAdapter(null,
            PaymentMethodsActivityStarter.Args.Builder()
                .build())
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertEquals("pm_3000", adapter.selectedPaymentMethod?.id)
    }

    @Test
    fun setPaymentMethods_whenInitialSpecified_selectsIt() {
        val adapter = PaymentMethodsAdapter("pm_1000",
            PaymentMethodsActivityStarter.Args.Builder()
                .build())
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertEquals("pm_1000", adapter.selectedPaymentMethod?.id)
    }
}
