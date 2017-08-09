package com.stripe.android.view;

import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
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
    private AppCompatTextView mCardInformationTextView;
    private AppCompatImageView mSelectedImageView;

    @Before
    public void setup() {
        ActivityController<CardInputTestActivity> activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start().resume();
        mMaskedCardView = activityController.get().getMaskedCardView();
        mCardInformationTextView = mMaskedCardView.findViewById(R.id.masked_card_info_view);
        mSelectedImageView = mMaskedCardView.findViewById(R.id.masked_check_icon);

        Calendar expirationCalendar = Calendar.getInstance();
        expirationCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
        expirationCalendar.set(Calendar.YEAR, 2050);
        Calendar nowCalendar = Calendar.getInstance();

        assertTrue("These tests assume that an expiry date of December 2050 is valid.",
                expirationCalendar.after(nowCalendar));
    }

    @Test
    public void setCard_setsCorrectData() {
        Card card = new Card(CardInputTestActivity.VALID_AMEX_NO_SPACES, 12, 50, "1234");
        mMaskedCardView.setCard(card);
        assertEquals("0005", mMaskedCardView.getLast4());
        assertEquals(Card.AMERICAN_EXPRESS, mMaskedCardView.getCardBrand());
        assertFalse(mMaskedCardView.isHighlighted());
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
        assertFalse(mMaskedCardView.isHighlighted());
    }

    @Test
    public void setCustomerSource_withNonCardSource_setsNoData() {
        CustomerSource customerSource = CustomerSource.fromString(EXAMPLE_JSON_SOURCE_BITCOIN);
        assertNotNull(customerSource);
        assertNotNull(customerSource.asSource());

        mMaskedCardView.setCustomerSource(customerSource);
        assertNull(mMaskedCardView.getCardBrand());
        assertNull(mMaskedCardView.getLast4());
        assertFalse(mMaskedCardView.isHighlighted());
    }

    @Test
    public void setCustomerSource_withCardSource_setsCorrectData() {
        CustomerSource customerSource = CustomerSource.fromString(EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(customerSource);
        assertNotNull(customerSource.asSource());

        mMaskedCardView.setCustomerSource(customerSource);
        assertEquals(Card.VISA, mMaskedCardView.getCardBrand());
        assertEquals("4242", mMaskedCardView.getLast4());
        assertFalse(mMaskedCardView.isHighlighted());
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
        assertFalse(mMaskedCardView.isHighlighted());
    }

    @Test
    public void setHighlighted_changesTextColor_andCheckMarkVisibility() {
        CustomerSource customerSource = CustomerSource.fromString(JSON_CARD);
        assertNotNull(customerSource);
        mMaskedCardView.setCustomerSource(customerSource);

        assertFalse(mMaskedCardView.isHighlighted());
        assertEquals(View.INVISIBLE, mSelectedImageView.getVisibility());
        assertEquals(mMaskedCardView.mUnselectedTextColorInt,
                mCardInformationTextView.getCurrentTextColor());

        mMaskedCardView.setHighlighted(true);

        assertTrue(mMaskedCardView.isHighlighted());
        assertEquals(View.VISIBLE, mSelectedImageView.getVisibility());
        assertEquals(mMaskedCardView.mSelectedColorInt,
                mCardInformationTextView.getCurrentTextColor());
    }

    @Test
    public void toggleHighlighted_switchesState() {
        CustomerSource customerSource = CustomerSource.fromString(JSON_CARD);
        assertNotNull(customerSource);
        mMaskedCardView.setCustomerSource(customerSource);
        assertFalse(mMaskedCardView.isHighlighted());

        mMaskedCardView.toggleHighlighted();
        assertTrue(mMaskedCardView.isHighlighted());
        assertEquals(View.VISIBLE, mSelectedImageView.getVisibility());

        mMaskedCardView.toggleHighlighted();
        assertFalse(mMaskedCardView.isHighlighted());
        assertEquals(View.INVISIBLE, mSelectedImageView.getVisibility());
    }
}
