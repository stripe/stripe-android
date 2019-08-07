package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.ObjectBuilder;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONObject;

import java.util.Objects;

/**
 * <a href="https://stripe.com/docs/sources/wechat-pay">WeChat Pay Payments with Sources</a>
 */
public final class WeChat extends StripeModel {
    private static final String FIELD_APPID = "android_appId";
    private static final String FIELD_NONCE = "android_nonceStr";
    private static final String FIELD_PACKAGE = "android_package";
    private static final String FIELD_PARTNERID = "android_partnerId";
    private static final String FIELD_PREPAYID = "android_prepayId";
    private static final String FIELD_SIGN = "android_sign";
    private static final String FIELD_TIMESTAMP = "android_timeStamp";
    private static final String FIELD_STATEMENT_DESCRIPTOR = "statement_descriptor";
    private static final String FIELD_QR_CODE_URL = "qr_code_url";

    @Nullable public final String statementDescriptor;
    @Nullable public final String appId;
    @Nullable public final String nonce;
    @Nullable public final String packageValue;
    @Nullable public final String partnerId;
    @Nullable public final String prepayId;
    @Nullable public final String sign;
    @Nullable public final String timestamp;
    @Nullable public final String qrCodeUrl;

    private WeChat(@NonNull Builder builder) {
        statementDescriptor = builder.statementDescriptor;
        appId = builder.appId;
        nonce = builder.nonce;
        packageValue = builder.packageValue;
        partnerId = builder.partnerId;
        prepayId = builder.prepayId;
        sign = builder.sign;
        timestamp = builder.timestamp;
        qrCodeUrl = builder.qrCodeUrl;
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(statementDescriptor, appId, nonce, packageValue, partnerId,
                prepayId, sign, timestamp);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof WeChat && typedEquals((WeChat) obj));
    }

    private boolean typedEquals(@NonNull WeChat obj) {
        return Objects.equals(statementDescriptor, obj.statementDescriptor) &&
                Objects.equals(appId, obj.appId) &&
                Objects.equals(nonce, obj.nonce) &&
                Objects.equals(packageValue, obj.packageValue) &&
                Objects.equals(partnerId, obj.partnerId) &&
                Objects.equals(prepayId, obj.prepayId) &&
                Objects.equals(sign, obj.sign) &&
                Objects.equals(timestamp, obj.timestamp) &&
                Objects.equals(qrCodeUrl, obj.qrCodeUrl);
    }

    @NonNull
    public static WeChat fromJson(@NonNull JSONObject json) {
        return new Builder()
                .setAppId(StripeJsonUtils.optString(json, FIELD_APPID))
                .setNonce(StripeJsonUtils.optString(json, FIELD_NONCE))
                .setPackageValue(StripeJsonUtils.optString(json, FIELD_PACKAGE))
                .setPartnerId(StripeJsonUtils.optString(json, FIELD_PARTNERID))
                .setPrepayId(StripeJsonUtils.optString(json, FIELD_PREPAYID))
                .setSign(StripeJsonUtils.optString(json, FIELD_SIGN))
                .setTimestamp(StripeJsonUtils.optString(json, FIELD_TIMESTAMP))
                .setStatementDescriptor(StripeJsonUtils.optString(json, FIELD_STATEMENT_DESCRIPTOR))
                .setQrCodeUrl(StripeJsonUtils.optString(json, FIELD_QR_CODE_URL))
                .build();
    }

    static final class Builder implements ObjectBuilder<WeChat> {
        private String statementDescriptor;
        private String appId;
        private String nonce;
        private String packageValue;
        private String partnerId;
        private String prepayId;
        private String sign;
        private String timestamp;
        private String qrCodeUrl;

        @NonNull
        Builder setStatementDescriptor(@Nullable String statementDescriptor) {
            this.statementDescriptor = statementDescriptor;
            return this;
        }

        @NonNull
        Builder setAppId(String appId) {
            this.appId = appId;
            return this;
        }

        @NonNull
        Builder setNonce(@Nullable String nonce) {
            this.nonce = nonce;
            return this;
        }

        @NonNull
        Builder setPackageValue(@Nullable String packageValue) {
            this.packageValue = packageValue;
            return this;
        }

        @NonNull
        Builder setPartnerId(@Nullable String partnerId) {
            this.partnerId = partnerId;
            return this;
        }

        @NonNull
        Builder setPrepayId(@Nullable String prepayId) {
            this.prepayId = prepayId;
            return this;
        }

        @NonNull
        Builder setSign(@Nullable String sign) {
            this.sign = sign;
            return this;
        }

        @NonNull
        Builder setTimestamp(@Nullable String timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @NonNull
        Builder setQrCodeUrl(@Nullable String qrCodeUrl) {
            this.qrCodeUrl = qrCodeUrl;
            return this;
        }

        @NonNull
        @Override
        public WeChat build() {
            return new WeChat(this);
        }
    }
}
