package com.stripe.android.view;

import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.R;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link MaskedCardView}
 */
@RunWith(RobolectricTestRunner.class)
public class MaskedCardViewTest {

    private MaskedCardView mMaskedCardView;
    private AppCompatImageView mSelectedImageView;

    @Before
    public void setup() {
        mMaskedCardView = new MaskedCardView(ApplicationProvider.getApplicationContext());
        mSelectedImageView = mMaskedCardView.findViewById(R.id.masked_check_icon);

        Calendar expirationCalendar = Calendar.getInstance();
        expirationCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
        expirationCalendar.set(Calendar.YEAR, 2050);
        Calendar nowCalendar = Calendar.getInstance();

        assertTrue("These tests assume that an expiry date of December 2050 is valid.",
                expirationCalendar.after(nowCalendar));
    }

    @Test
    public void init_setsColorValuesWithAlpha() {
        final int alpha = 204; // 80% of 255
        int[] colorValues = mMaskedCardView.getTextColorValues();
        // The colors are arranged [selected, selectedLowAlpha, unselected, unselectedLowAlpha
        assertEquals(4, colorValues.length);
        assertEquals(colorValues[1], ColorUtils.setAlphaComponent(colorValues[0], alpha));
        assertEquals(colorValues[3], ColorUtils.setAlphaComponent(colorValues[2], alpha));
    }

    @Test
    public void setPaymentMethod_setsCorrectData() {
        final PaymentMethod paymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        assertNotNull(paymentMethod);
        mMaskedCardView.setPaymentMethod(paymentMethod);
        assertEquals("4242", mMaskedCardView.getLast4());
        assertEquals(PaymentMethod.Card.Brand.VISA, mMaskedCardView.getCardBrand());
        assertFalse(mMaskedCardView.isSelected());
    }

    @Test
    public void setSelected_changesCheckMarkVisibility() {
        final PaymentMethod paymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        assertNotNull(paymentMethod);
        mMaskedCardView.setPaymentMethod(paymentMethod);

        assertFalse(mMaskedCardView.isSelected());
        assertEquals(View.INVISIBLE, mSelectedImageView.getVisibility());

        mMaskedCardView.setSelected(true);

        assertTrue(mMaskedCardView.isSelected());
        assertEquals(View.VISIBLE, mSelectedImageView.getVisibility());
    }

    @Test
    public void toggleSelected_switchesState() {
        final PaymentMethod paymentMethod =
                PaymentMethod.fromString(PaymentMethodTest.PM_CARD_JSON);
        assertNotNull(paymentMethod);
        mMaskedCardView.setPaymentMethod(paymentMethod);
        assertFalse(mMaskedCardView.isSelected());

        mMaskedCardView.toggleSelected();
        assertTrue(mMaskedCardView.isSelected());
        assertEquals(View.VISIBLE, mSelectedImageView.getVisibility());

        mMaskedCardView.toggleSelected();
        assertFalse(mMaskedCardView.isSelected());
        assertEquals(View.INVISIBLE, mSelectedImageView.getVisibility());
    }

    @Test
    public void whenSourceNotCard_doesNotCrash() {
        final PaymentMethod paymentMethod = new PaymentMethod.Builder().build();

        mMaskedCardView.setPaymentMethod(paymentMethod);
    }
}
