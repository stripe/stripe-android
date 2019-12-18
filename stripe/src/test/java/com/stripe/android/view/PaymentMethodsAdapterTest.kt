package com.stripe.android.view

import android.content.Context
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.model.PaymentMethod
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

    @Mock
    private lateinit var listener: PaymentMethodsAdapter.Listener

    private val paymentMethodsAdapter: PaymentMethodsAdapter = PaymentMethodsAdapter(ARGS)

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }

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
        paymentMethodsAdapter
        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertNull(paymentMethodsAdapter.selectedPaymentMethod)
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

    @Test
    fun testGetItemViewType_withGooglePayEnabled() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            addableTypes = listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx),
            shouldShowGooglePay = true
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        assertEquals(6, adapter.itemCount)

        assertEquals(
            PaymentMethodsAdapter.ViewType.GooglePay.ordinal,
            adapter.getItemViewType(0)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.Card.ordinal,
            adapter.getItemViewType(1)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.Card.ordinal,
            adapter.getItemViewType(2)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.Card.ordinal,
            adapter.getItemViewType(3)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.AddCard.ordinal,
            adapter.getItemViewType(4)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.AddFpx.ordinal,
            adapter.getItemViewType(5)
        )
    }

    @Test
    fun testGetItemId_withGooglePayEnabled() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            addableTypes = listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx),
            shouldShowGooglePay = true
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        assertEquals(6, adapter.itemCount)

        assertEquals(
            PaymentMethodsAdapter.GOOGLE_PAY_ITEM_ID,
            adapter.getItemId(0)
        )

        assertEquals(
            643895348L,
            adapter.getItemId(1)
        )

        assertEquals(
            91030075L,
            adapter.getItemId(2)
        )

        assertEquals(
            -420206809L,
            adapter.getItemId(3)
        )

        assertEquals(
            3046160L,
            adapter.getItemId(4)
        )

        assertEquals(
            101614L,
            adapter.getItemId(5)
        )
    }

    @Test
    fun testGetItemViewType_withGooglePayDisabled() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            addableTypes = listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx),
            shouldShowGooglePay = false
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        assertEquals(5, adapter.itemCount)

        assertEquals(
            PaymentMethodsAdapter.ViewType.Card.ordinal,
            adapter.getItemViewType(0)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.Card.ordinal,
            adapter.getItemViewType(1)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.Card.ordinal,
            adapter.getItemViewType(2)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.AddCard.ordinal,
            adapter.getItemViewType(3)
        )

        assertEquals(
            PaymentMethodsAdapter.ViewType.AddFpx.ordinal,
            adapter.getItemViewType(4)
        )
    }

    @Test
    fun testGooglePayRowClick_shouldCallListener() {
        val adapter = PaymentMethodsAdapter(ARGS, shouldShowGooglePay = true)
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        adapter.listener = listener

        val itemView = FrameLayout(context)
        val viewHolder = PaymentMethodsAdapter.ViewHolder.GooglePayViewHolder(itemView)
        adapter.onBindViewHolder(viewHolder, 0)

        itemView.performClick()
        verify(listener).onGooglePayClick()
    }

    private companion object {
        private val ARGS =
            PaymentMethodsActivityStarter.Args.Builder()
                .build()
    }
}
