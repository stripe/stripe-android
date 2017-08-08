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

    private static final String EXAMPLE_JSON_SOURCE_CARD_DATA =
            "{\"exp_month\":12,\"exp_year\":2050," +
                    "\"address_line1_check\":\"unchecked\",\"address_zip_check\":" +
                    "\"unchecked\",\"brand\":\"Visa\",\"country\":\"US\",\"cvc_check\"" +
                    ":\"unchecked\",\"funding\":\"credit\",\"last4\":\"4242\",\"three_d_secure\"" +
                    ":\"optional\"}";

    private static final String EXAMPLE_JSON_CARD_SOURCE = "{\n"+
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n"+
            "\"object\": \"source\",\n"+
            "\"amount\": 1000,\n"+
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n"+
            "\"created\": 1488499654,\n"+
            "\"currency\": \"usd\",\n"+
            "\"flow\": \"receiver\",\n"+
            "\"livemode\": false,\n"+
            "\"metadata\": {\n"+
            "},\n"+
            "\"owner\": {\n"+
            "\"address\": null,\n"+
            "\"email\": \"jenny.rosen@example.com\",\n"+
            "\"name\": \"Jenny Rosen\",\n"+
            "\"phone\": \"4158675309\",\n"+
            "\"verified_address\": null,\n"+
            "\"verified_email\": null,\n"+
            "\"verified_name\": null,\n"+
            "\"verified_phone\": null\n"+
            "},\n"+
            "\"receiver\": {\n"+
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n"+
            "\"amount_charged\": 0,\n"+
            "\"amount_received\": 0,\n"+
            "\"amount_returned\": 0\n"+
            "},\n"+
            "\"status\": \"pending\",\n"+
            "\"type\": \"card\",\n"+
            "\"usage\": \"single_use\",\n"+
            "\"card\": " + EXAMPLE_JSON_SOURCE_CARD_DATA + "\n"+
            "}";

    private static final String EXAMPLE_JSON_SOURCE_BITCOIN = "{\n"+
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n"+
            "\"object\": \"source\",\n"+
            "\"amount\": 1000,\n"+
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n"+
            "\"created\": 1488499654,\n"+
            "\"currency\": \"usd\",\n"+
            "\"flow\": \"receiver\",\n"+
            "\"livemode\": false,\n"+
            "\"metadata\": {\n"+
            "},\n"+
            "\"owner\": {\n"+
            "\"address\": null,\n"+
            "\"email\": \"jenny.rosen@example.com\",\n"+
            "\"name\": \"Jenny Rosen\",\n"+
            "\"phone\": \"4158675309\",\n"+
            "\"verified_address\": null,\n"+
            "\"verified_email\": null,\n"+
            "\"verified_name\": null,\n"+
            "\"verified_phone\": null\n"+
            "},\n"+
            "\"receiver\": {\n"+
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n"+
            "\"amount_charged\": 0,\n"+
            "\"amount_received\": 0,\n"+
            "\"amount_returned\": 0\n"+
            "},\n"+
            "\"status\": \"pending\",\n"+
            "\"type\": \"bitcoin\",\n"+
            "\"usage\": \"single_use\"\n"+
            "}";

    static final String JSON_CARD = "{\n" +
            "    \"id\": \"card_189fi32eZvKYlo2CHK8NPRME\",\n" +
            "    \"object\": \"card\",\n" +
            "    \"address_city\": \"Des Moines\",\n" +
            "    \"address_country\": \"US\",\n" +
            "    \"address_line1\": \"123 Any Street\",\n" +
            "    \"address_line1_check\": \"unavailable\",\n" +
            "    \"address_line2\": \"456\",\n" +
            "    \"address_state\": \"IA\",\n" +
            "    \"address_zip\": \"50305\",\n" +
            "    \"address_zip_check\": \"unavailable\",\n" +
            "    \"brand\": \"MasterCard\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"currency\": \"usd\",\n" +
            "    \"customer\": \"customer77\",\n" +
            "    \"cvc_check\": \"unavailable\",\n" +
            "    \"exp_month\": 8,\n" +
            "    \"exp_year\": 2017,\n" +
            "    \"funding\": \"credit\",\n" +
            "    \"fingerprint\": \"abc123\",\n" +
            "    \"last4\": \"5555\",\n" +
            "    \"name\": \"John Cardholder\"\n" +
            "  }";

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
        assertEquals(mMaskedCardView.mUnselectedColorInt,
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
