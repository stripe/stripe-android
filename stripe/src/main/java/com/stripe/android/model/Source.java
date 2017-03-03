package com.stripe.android.model;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A model class representing a source in the Android SDK. More detailed information
 * and interaction can be seen at {@url https://stripe.com/docs/api/java#source_object}.
 */
public class Source {

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            BITCOIN,
            CARD,
            THREE_D_SECURE,
            GIROPAY,
            SEPA_DEBIT,
            IDEAL,
            SOFORT,
            BANCONTACT
    })
    public @interface SourceType { }
    public static final String BITCOIN = "bitcoin";
    public static final String CARD = "card";
    public static final String THREE_D_SECURE = "three_d_secure";
    public static final String GIROPAY = "giropay";
    public static final String SEPA_DEBIT = "sepa_debit";
    public static final String IDEAL = "ideal";
    public static final String SOFORT = "sofort";
    public static final String BANCONTACT = "bancontact";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            PENDING,
            CHARGEABLE,
            CONSUMED,
            CANCELED
    })
    public @interface SourceStatus { }
    public static final String PENDING = "pending";
    public static final String CHARGEABLE = "chargeable";
    public static final String CONSUMED = "consumed";
    public static final String CANCELED = "canceled";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            REUSABLE,
            SINGLE_USE
    })
    public @interface Usage { }
    public static final String REUSABLE = "reusable";
    public static final String SINGLE_USE = "single_use";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            REDIRECT,
            RECEIVER,
            CODE_VERIFICATION,
            NONE
    })
    public @interface SourceFlow { }
    public static final String REDIRECT = "redirect";
    public static final String RECEIVER = "receiver";
    public static final String CODE_VERIFICATION = "code_verification";
    public static final String NONE = "none";

    static final String EURO = "eur";
    static final String USD = "usd";

    private SourceType mType;
    private SourceStatus mStatus;
    private Usage mUsage;
}
