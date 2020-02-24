package com.stripe.android.view

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [MaskedCardView]
 */
@RunWith(RobolectricTestRunner::class)
class MaskedCardViewTest {

    private val maskedCardView = MaskedCardView(ApplicationProvider.getApplicationContext<Context>())
    private val selectedImageView: ImageView = maskedCardView.findViewById(R.id.check_icon)

    @BeforeTest
    fun setup() {
        val expirationCalendar = Calendar.getInstance().also {
            it.set(Calendar.MONTH, Calendar.DECEMBER)
            it.set(Calendar.YEAR, 2050)
        }
        val nowCalendar = Calendar.getInstance()

        assertTrue(
            expirationCalendar.after(nowCalendar),
            "These tests assume that an expiry date of December 2050 is valid."
        )
    }

    @Test
    fun init_setsColorValuesWithAlpha() {
        val alpha = 204 // 80% of 255
        val colorValues = maskedCardView.textColorValues
        // The colors are arranged [selected, selectedLowAlpha, unselected, unselectedLowAlpha
        assertEquals(4, colorValues.size)
        assertEquals(colorValues[1],
            ColorUtils.setAlphaComponent(colorValues[0], alpha))
        assertEquals(colorValues[3],
            ColorUtils.setAlphaComponent(colorValues[2], alpha))
    }

    @Test
    fun setPaymentMethod_setsCorrectData() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        maskedCardView.setPaymentMethod(paymentMethod)
        assertEquals("4242", maskedCardView.last4)
        assertEquals(CardBrand.Visa, maskedCardView.cardBrand)
        assertFalse(maskedCardView.isSelected)
    }

    @Test
    fun setSelected_changesCheckMarkVisibility() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        maskedCardView.setPaymentMethod(paymentMethod)

        assertFalse(maskedCardView.isSelected)
        assertEquals(View.INVISIBLE, selectedImageView.visibility)

        maskedCardView.isSelected = true

        assertTrue(maskedCardView.isSelected)
        assertEquals(View.VISIBLE, selectedImageView.visibility)
    }

    @Test
    fun whenTypeNotCard_doesNotCrash() {
        maskedCardView.setPaymentMethod(PaymentMethodFixtures.FPX_PAYMENT_METHOD)
    }
}
