package com.stripe.android.view;

import com.stripe.android.BuildConfig;
import com.stripe.android.model.Card;
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

    private static final String EXAMPLE_JSON_SOURCE_WITH_NULLS = "{\n"+
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

    private MaskedCardView mMaskedCardView;

    @Before
    public void setup() {
        ActivityController<CardInputTestActivity> activityController =
                Robolectric.buildActivity(CardInputTestActivity.class).create().start();
        mMaskedCardView = activityController.get().getMaskedCardView();
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
        assertFalse(mMaskedCardView.getIsSelected());
    }

    @Test
    public void setCardData_setsCorrectData() {
        Source source = Source.fromString(EXAMPLE_JSON_SOURCE_WITH_NULLS);
        assertNotNull(source);
        assertTrue(source.getSourceTypeModel() instanceof SourceCardData);
        SourceCardData sourceCardData = (SourceCardData) source.getSourceTypeModel();
        assertNotNull(sourceCardData);

        mMaskedCardView.setCardData(sourceCardData);
        assertEquals(Card.VISA, mMaskedCardView.getCardBrand());
        assertEquals("4242", mMaskedCardView.getLast4());
        assertFalse(mMaskedCardView.getIsSelected());
    }
}
