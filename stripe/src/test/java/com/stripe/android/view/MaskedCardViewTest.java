package com.stripe.android.view;

import android.support.v4.graphics.ColorUtils;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;

import com.stripe.android.BuildConfig;
import com.stripe.android.R;
import com.stripe.android.model.Card;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Calendar;

import static com.stripe.android.view.CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE;
import static com.stripe.android.view.CardInputTestActivity.EXAMPLE_JSON_SOURCE_BITCOIN;
import static com.stripe.android.view.CardInputTestActivity.JSON_CARD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link MaskedCardView}
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class MaskedCardViewTest {

    private MaskedCardView mMaskedCardView;
    private AppCompatImageView mSelectedImageView;

    @Before
    public void setup() {
        ActivityController<CardInputTestActivity> activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start().resume();
        mMaskedCardView = activityController.get().getMaskedCardView();
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
    public void setCard_setsCorrectData() {
        Card card = new Card(CardInputTestActivity.VALID_AMEX_NO_SPACES, 12, 50, "1234");
        mMaskedCardView.setCard(card);
        assertEquals("0005", mMaskedCardView.getLast4());
        assertEquals(Card.AMERICAN_EXPRESS, mMaskedCardView.getCardBrand());
        assertFalse(mMaskedCardView.isSelected());
    }

    @Test
    public void setSourceCardData_withCardSource_setsCorrectData() {
        Source source = Source.fromString(EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(source);
        assertTrue(source.getSourceTypeModel() instanceof SourceCardData);
        SourceCardData sourceCardData = (SourceCardData) source.getSourceTypeModel();
        assertNotNull(sourceCardData);

        mMaskedCardView.setSourceCardData(sourceCardData);
        assertEquals(Card.VISA, mMaskedCardView.getCardBrand());
        assertEquals("4242", mMaskedCardView.getLast4());
        assertFalse(mMaskedCardView.isSelected());
    }

    @Test
    public void setCustomerSource_withNonCardSource_setsNoData() {
        CustomerSource customerSource = CustomerSource.fromString(EXAMPLE_JSON_SOURCE_BITCOIN);
        assertNotNull(customerSource);
        assertNotNull(customerSource.asSource());

        mMaskedCardView.setCustomerSource(customerSource);
        assertNull(mMaskedCardView.getCardBrand());
        assertNull(mMaskedCardView.getLast4());
        assertFalse(mMaskedCardView.isSelected());
    }

    @Test
    public void setCustomerSource_withCardSource_setsCorrectData() {
        CustomerSource customerSource = CustomerSource.fromString(EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(customerSource);
        assertNotNull(customerSource.asSource());

        mMaskedCardView.setCustomerSource(customerSource);
        assertEquals(Card.VISA, mMaskedCardView.getCardBrand());
        assertEquals("4242", mMaskedCardView.getLast4());
        assertFalse(mMaskedCardView.isSelected());
    }

    @Test
    public void setCustomerSource_withCardObject_setsCorrectData() {
        CustomerSource customerSource = CustomerSource.fromString(JSON_CARD);
        assertNotNull(customerSource);
        assertNotNull(customerSource.asCard());
        assertNull(customerSource.asSource());

        mMaskedCardView.setCustomerSource(customerSource);
        assertEquals(Card.MASTERCARD, mMaskedCardView.getCardBrand());
        assertEquals("5555", mMaskedCardView.getLast4());
        assertFalse(mMaskedCardView.isSelected());
    }

    @Test
    public void setSelected_changesCheckMarkVisibility() {
        CustomerSource customerSource = CustomerSource.fromString(JSON_CARD);
        assertNotNull(customerSource);
        mMaskedCardView.setCustomerSource(customerSource);

        assertFalse(mMaskedCardView.isSelected());
        assertEquals(View.INVISIBLE, mSelectedImageView.getVisibility());

        mMaskedCardView.setSelected(true);

        assertTrue(mMaskedCardView.isSelected());
        assertEquals(View.VISIBLE, mSelectedImageView.getVisibility());
    }

    @Test
    public void toggleSelected_switchesState() {
        CustomerSource customerSource = CustomerSource.fromString(JSON_CARD);
        assertNotNull(customerSource);
        mMaskedCardView.setCustomerSource(customerSource);
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
        SourceCardData sourceCardData = Mockito.mock(SourceCardData.class);
        Mockito.when(sourceCardData.getBrand()).thenReturn("unrecognized_brand");
        Mockito.when(sourceCardData.getLast4()).thenReturn("");
        mMaskedCardView.setSourceCardData(sourceCardData);
    }
}
