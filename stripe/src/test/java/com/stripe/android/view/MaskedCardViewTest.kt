package com.stripe.android.view

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.R
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodTest
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import java.util.Calendar
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [MaskedCardView]
 */
@RunWith(RobolectricTestRunner::class)
class MaskedCardViewTest {

    private lateinit var maskedCardView: MaskedCardView
    private lateinit var selectedImageView: AppCompatImageView

    @BeforeTest
    fun setup() {
        maskedCardView = MaskedCardView(ApplicationProvider.getApplicationContext<Context>())
        selectedImageView = maskedCardView.findViewById(R.id.masked_check_icon)

        val expirationCalendar = Calendar.getInstance()
        expirationCalendar.set(Calendar.MONTH, Calendar.DECEMBER)
        expirationCalendar.set(Calendar.YEAR, 2050)
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
        val paymentMethod = PaymentMethodJsonParser().parse(PaymentMethodTest.PM_CARD_JSON)
        assertNotNull(paymentMethod)
        maskedCardView.setPaymentMethod(paymentMethod)
        assertEquals("4242", maskedCardView.last4)
        assertEquals(PaymentMethod.Card.Brand.VISA, maskedCardView.cardBrand)
        assertFalse(maskedCardView.isSelected)
    }

    @Test
    fun setSelected_changesCheckMarkVisibility() {
        val paymentMethod = PaymentMethodJsonParser().parse(PaymentMethodTest.PM_CARD_JSON)
        assertNotNull(paymentMethod)
        maskedCardView.setPaymentMethod(paymentMethod)

        assertFalse(maskedCardView.isSelected)
        assertEquals(View.INVISIBLE, selectedImageView.visibility)

        maskedCardView.isSelected = true

        assertTrue(maskedCardView.isSelected)
        assertEquals(View.VISIBLE, selectedImageView.visibility)
    }

    @Test
    fun whenSourceNotCard_doesNotCrash() {
        val paymentMethod = PaymentMethod.Builder().build()
        maskedCardView.setPaymentMethod(paymentMethod)
    }
}
