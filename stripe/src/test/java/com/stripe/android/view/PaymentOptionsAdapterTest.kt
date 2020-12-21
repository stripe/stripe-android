package com.stripe.android.view

import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test class for [PaymentMethodsAdapter]
 */
@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
class PaymentOptionsAdapterTest {
    private val adapterDataObserver: RecyclerView.AdapterDataObserver = mock()
    private val listener: PaymentMethodsAdapter.Listener = mock()

    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
    )
    private val testDispatcher = TestCoroutineDispatcher()

    private val parentView = FrameLayout(context)

    private val paymentMethodsAdapter = PaymentMethodsAdapter(
        ARGS,
        workContext = testDispatcher
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        paymentMethodsAdapter.registerAdapterDataObserver(adapterDataObserver)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun setSelection_changesSelection() {
        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertThat(paymentMethodsAdapter.itemCount)
            .isEqualTo(4)
        verify(adapterDataObserver).onChanged()

        paymentMethodsAdapter.selectedPaymentMethodId = paymentMethodsAdapter.paymentMethods[2].id
        assertThat(requireNotNull(paymentMethodsAdapter.selectedPaymentMethod).id)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id)

        paymentMethodsAdapter.selectedPaymentMethodId =
            PaymentMethodFixtures.CARD_PAYMENT_METHODS[1].id
        assertThat(PaymentMethodFixtures.CARD_PAYMENT_METHODS[1].id)
            .isEqualTo(paymentMethodsAdapter.selectedPaymentMethod?.id)
    }

    @Test
    fun updatePaymentMethods_removesExistingPaymentMethodsAndAddsAllPaymentMethods() {
        val singlePaymentMethod =
            listOf(PaymentMethodFixtures.CARD_PAYMENT_METHODS[0])

        paymentMethodsAdapter.setPaymentMethods(singlePaymentMethod)
        paymentMethodsAdapter.selectedPaymentMethodId = paymentMethodsAdapter.paymentMethods[0].id
        assertThat(paymentMethodsAdapter.itemCount)
            .isEqualTo(2)
        assertThat(paymentMethodsAdapter.selectedPaymentMethod)
            .isNotNull()

        assertThat(paymentMethodsAdapter.selectedPaymentMethod?.id)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHODS[0].id)

        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        paymentMethodsAdapter.selectedPaymentMethodId = paymentMethodsAdapter.paymentMethods[2].id
        assertThat(paymentMethodsAdapter.itemCount)
            .isEqualTo(4)
        assertThat(paymentMethodsAdapter.selectedPaymentMethod?.id)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id)
        verify(adapterDataObserver, times(2))
            .onChanged()
    }

    @Test
    fun updatePaymentMethods_withSelection_updatesPaymentMethodsAndSelectionMaintained() {
        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertThat(paymentMethodsAdapter.itemCount)
            .isEqualTo(4)
        paymentMethodsAdapter.selectedPaymentMethodId = PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id
        assertThat(paymentMethodsAdapter.selectedPaymentMethod)
            .isNotNull()

        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertThat(paymentMethodsAdapter.itemCount)
            .isEqualTo(4)
        assertThat(paymentMethodsAdapter.selectedPaymentMethod?.id)
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHODS[2].id)
    }

    @Test
    fun setPaymentMethods_whenNoInitialSpecified_returnsNull() {
        paymentMethodsAdapter
        paymentMethodsAdapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertThat(paymentMethodsAdapter.selectedPaymentMethod)
            .isNull()
    }

    @Test
    fun setPaymentMethods_whenInitialSpecified_selectsIt() {
        val adapter = PaymentMethodsAdapter(
            intentArgs = PaymentMethodsActivityStarter.Args.Builder()
                .build(),
            initiallySelectedPaymentMethodId = "pm_1000",
            workContext = testDispatcher
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertThat(adapter.selectedPaymentMethod?.id)
            .isEqualTo("pm_1000")
    }

    @Test
    fun testGetItemViewType_withGooglePayEnabled() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            addableTypes = listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx),
            shouldShowGooglePay = true,
            workContext = testDispatcher
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        assertThat(adapter.itemCount)
            .isEqualTo(6)

        assertThat(
            adapter.viewTypes
        ).isEqualTo(
            listOf(
                PaymentMethodsAdapter.ViewType.GooglePay,
                PaymentMethodsAdapter.ViewType.Card,
                PaymentMethodsAdapter.ViewType.Card,
                PaymentMethodsAdapter.ViewType.Card,
                PaymentMethodsAdapter.ViewType.AddCard,
                PaymentMethodsAdapter.ViewType.AddFpx
            )
        )
    }

    @Test
    fun testGetItemId_withGooglePayEnabled() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            addableTypes = listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx),
            shouldShowGooglePay = true,
            workContext = testDispatcher
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        assertThat(adapter.itemCount)
            .isEqualTo(6)

        assertThat(adapter.getItemId(0))
            .isEqualTo(PaymentMethodsAdapter.GOOGLE_PAY_ITEM_ID)

        val uniqueItemIds = (1..5)
            .map { adapter.getItemId(it) }
            .toSet()
        assertThat(uniqueItemIds)
            .hasSize(5)
    }

    @Test
    fun testGetItemViewType_withGooglePayDisabled() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            addableTypes = listOf(PaymentMethod.Type.Card, PaymentMethod.Type.Fpx),
            shouldShowGooglePay = false,
            workContext = testDispatcher
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        assertThat(adapter.viewTypes)
            .isEqualTo(
                listOf(
                    PaymentMethodsAdapter.ViewType.Card,
                    PaymentMethodsAdapter.ViewType.Card,
                    PaymentMethodsAdapter.ViewType.Card,
                    PaymentMethodsAdapter.ViewType.AddCard,
                    PaymentMethodsAdapter.ViewType.AddFpx
                )
            )
    }

    @Test
    fun testGooglePayRowClick_shouldCallListener() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            shouldShowGooglePay = true,
            workContext = testDispatcher
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        adapter.listener = listener

        val viewHolder = PaymentMethodsAdapter.ViewHolder.GooglePayViewHolder(context, parentView)
        adapter.onBindViewHolder(viewHolder, 0)

        viewHolder.itemView.performClick()
        verify(listener).onGooglePayClick()
    }

    @Test
    fun `onPositionClicked() should call listener's onPaymentMethodClick()`() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            workContext = testDispatcher
        )

        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        adapter.listener = listener
        adapter.onPositionClicked(0)

        verify(listener).onPaymentMethodClick(PaymentMethodFixtures.CARD_PAYMENT_METHODS.first())
    }

    @Test
    fun getPosition_withValidPaymentMethod_returnsPosition() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            shouldShowGooglePay = true,
            workContext = testDispatcher
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        assertThat(adapter.getPosition(PaymentMethodFixtures.CARD_PAYMENT_METHODS.last()))
            .isEqualTo(3)
    }

    @Test
    fun getPosition_withInvalidPaymentMethod_returnsNull() {
        val adapter = PaymentMethodsAdapter(
            ARGS,
            shouldShowGooglePay = true,
            workContext = testDispatcher
        )
        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)

        assertThat(adapter.getPosition(PaymentMethodFixtures.FPX_PAYMENT_METHOD))
            .isNull()
    }

    @Test
    fun deletePaymentMethod_withValidPaymentMethod_removesPaymentMethod() {
        val adapter = PaymentMethodsAdapter(ARGS, shouldShowGooglePay = true)
        adapter.registerAdapterDataObserver(adapterDataObserver)

        adapter.setPaymentMethods(PaymentMethodFixtures.CARD_PAYMENT_METHODS)
        assertThat(adapter.paymentMethods)
            .contains(PaymentMethodFixtures.CARD_PAYMENT_METHODS.last())
        adapter.deletePaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHODS.last())
        assertThat(adapter.paymentMethods)
            .doesNotContain(PaymentMethodFixtures.CARD_PAYMENT_METHODS.last())
        verify(adapterDataObserver).onItemRangeRemoved(3, 1)
    }

    @Test
    fun `multiple clicks on add card view should emit single args`() {
        val adapter = PaymentMethodsAdapter(ARGS, shouldShowGooglePay = true)

        val argsList = mutableListOf<AddPaymentMethodActivityStarter.Args>()
        adapter.addPaymentMethodArgs.observeForever { args ->
            if (args != null) {
                argsList.add(args)
            }
        }

        val viewHolder = adapter.createViewHolder(
            FrameLayout(context),
            PaymentMethodsAdapter.ViewType.AddCard.ordinal
        )
        adapter.onBindViewHolder(viewHolder, 0)
        viewHolder.itemView.performClick()
        viewHolder.itemView.performClick()
        viewHolder.itemView.performClick()

        assertThat(argsList)
            .containsExactly(adapter.addCardArgs)
    }

    @Test
    fun `multiple clicks on add FPX view should emit single args`() {
        val adapter = PaymentMethodsAdapter(ARGS, shouldShowGooglePay = true)

        val argsList = mutableListOf<AddPaymentMethodActivityStarter.Args>()
        adapter.addPaymentMethodArgs.observeForever { args ->
            if (args != null) {
                argsList.add(args)
            }
        }

        val viewHolder = adapter.createViewHolder(
            FrameLayout(context),
            PaymentMethodsAdapter.ViewType.AddFpx.ordinal
        )
        adapter.onBindViewHolder(viewHolder, 0)
        viewHolder.itemView.performClick()
        viewHolder.itemView.performClick()
        viewHolder.itemView.performClick()

        assertThat(argsList)
            .containsExactly(adapter.addFpxArgs)
    }

    private companion object {
        private val ARGS =
            PaymentMethodsActivityStarter.Args.Builder()
                .build()

        private val PaymentMethodsAdapter.viewTypes: List<PaymentMethodsAdapter.ViewType>
            get() {
                return (0 until itemCount).map {
                    PaymentMethodsAdapter.ViewType.values()[getItemViewType(it)]
                }
            }
    }
}
