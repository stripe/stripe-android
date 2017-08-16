package com.stripe.android.view;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;

import com.stripe.android.R;

/**
 * Activity used to test UI components. We add the layout programmatically to avoid needing test
 * resource files.
 */
public class CardInputTestActivity extends AppCompatActivity {

    public static final String VALID_AMEX_NO_SPACES = "378282246310005";
    public static final String VALID_AMEX_WITH_SPACES = "3782 822463 10005";
    public static final String VALID_DINERS_CLUB_NO_SPACES = "30569309025904";
    public static final String VALID_DINERS_CLUB_WITH_SPACES = "3056 9309 0259 04";
    public static final String VALID_VISA_NO_SPACES = "4242424242424242";
    public static final String VALID_VISA_WITH_SPACES = "4242 4242 4242 4242";

    static final String EXAMPLE_JSON_SOURCE_CARD_DATA =
            "{\"exp_month\":12,\"exp_year\":2050," +
                    "\"address_line1_check\":\"unchecked\",\"address_zip_check\":" +
                    "\"unchecked\",\"brand\":\"Visa\",\"country\":\"US\",\"cvc_check\"" +
                    ":\"unchecked\",\"funding\":\"credit\",\"last4\":\"4242\",\"three_d_secure\"" +
                    ":\"optional\"}";

    public static final String EXAMPLE_JSON_CARD_SOURCE = "{\n"+
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

    public static final String EXAMPLE_JSON_CARD_SOURCE_SECOND = "{\n"+
            "\"id\": \"src_20s4yLCArFYluyI4uz2dxBgR\",\n"+
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

    static final String EXAMPLE_JSON_SOURCE_BITCOIN = "{\n"+
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

    private CardInputWidget mCardInputWidget;
    private CardMultilineWidget mCardMultilineWidget;
    private CardMultilineWidget mNoZipCardMulitlineWidget;
    private MaskedCardView mMaskedCardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.StripeDefaultTheme);
        mCardInputWidget = new CardInputWidget(this);
        mCardMultilineWidget = new CardMultilineWidget(this, true);
        mNoZipCardMulitlineWidget = new CardMultilineWidget(this, false);
        mMaskedCardView = new MaskedCardView(this);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.addView(mCardInputWidget);
        linearLayout.addView(mCardMultilineWidget);
        linearLayout.addView(mNoZipCardMulitlineWidget);
        linearLayout.addView(mMaskedCardView);
        setContentView(linearLayout);
    }

    public CardNumberEditText getCardNumberEditText() {
        return (CardNumberEditText) mCardInputWidget.findViewById(R.id.et_card_number);
    }

    public ExpiryDateEditText getExpiryDateEditText() {
        return (ExpiryDateEditText) mCardInputWidget.findViewById(R.id.et_expiry_date);
    }

    public StripeEditText getCvcEditText() {
        return (StripeEditText) mCardInputWidget.findViewById(R.id.et_cvc_number);
    }

    public CardInputWidget getCardInputWidget() {
        return mCardInputWidget;
    }

    public CardMultilineWidget getCardMultilineWidget() {
        return mCardMultilineWidget;
    }

    public MaskedCardView getMaskedCardView() {
        return mMaskedCardView;
    }

    public CardMultilineWidget getNoZipCardMulitlineWidget() {
        return mNoZipCardMulitlineWidget;
    }
}
