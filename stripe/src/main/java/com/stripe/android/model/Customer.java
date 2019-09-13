package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.stripe.android.model.StripeJsonUtils.optBoolean;
import static com.stripe.android.model.StripeJsonUtils.optInteger;
import static com.stripe.android.model.StripeJsonUtils.optString;

/**
 * Model for a Stripe Customer object
 */
public final class Customer extends StripeModel {

    private static final String FIELD_ID = "id";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_DEFAULT_SOURCE = "default_source";
    private static final String FIELD_SHIPPING = "shipping";
    private static final String FIELD_SOURCES = "sources";

    private static final String FIELD_DATA = "data";
    private static final String FIELD_HAS_MORE = "has_more";
    private static final String FIELD_TOTAL_COUNT = "total_count";
    private static final String FIELD_URL = "url";

    private static final String VALUE_LIST = "list";
    private static final String VALUE_CUSTOMER = "customer";

    private static final String VALUE_APPLE_PAY = "apple_pay";

    @Nullable private final String mId;
    @Nullable private final String mDefaultSource;
    @Nullable private final ShippingInformation mShippingInformation;
    @NonNull private final List<CustomerSource> mSources;
    @Nullable private final Boolean mHasMore;
    @Nullable private final Integer mTotalCount;
    @Nullable private final String mUrl;

    private Customer(@Nullable String id, @Nullable String defaultSource,
                     @Nullable ShippingInformation shippingInformation,
                     @NonNull List<CustomerSource> sources, @Nullable Boolean hasMore,
                     @Nullable Integer totalCount, @Nullable String url) {
        mId = id;
        mDefaultSource = defaultSource;
        mShippingInformation = shippingInformation;
        mSources = sources;
        mHasMore = hasMore;
        mTotalCount = totalCount;
        mUrl = url;
    }

    public String getId() {
        return mId;
    }

    public String getDefaultSource() {
        return mDefaultSource;
    }

    public ShippingInformation getShippingInformation() {
        return mShippingInformation;
    }

    @NonNull
    public List<CustomerSource> getSources() {
        return mSources;
    }

    @Nullable
    public Boolean getHasMore() {
        return mHasMore;
    }

    public Integer getTotalCount() {
        return mTotalCount;
    }

    public String getUrl() {
        return mUrl;
    }

    @Nullable
    public CustomerSource getSourceById(@NonNull String sourceId) {
        for (CustomerSource source : mSources) {
            if (sourceId.equals(source.getId())) {
                return source;
            }
        }
        return null;
    }

    @Nullable
    public static Customer fromString(@Nullable String jsonString) {
        if (jsonString == null) {
            return null;
        }

        try {
            return fromJson(new JSONObject(jsonString));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static Customer fromJson(@NonNull JSONObject jsonObject) {
        String objectType = optString(jsonObject, FIELD_OBJECT);
        if (!VALUE_CUSTOMER.equals(objectType)) {
            return null;
        }
        final String id = optString(jsonObject, FIELD_ID);
        final String defaultSource = optString(jsonObject, FIELD_DEFAULT_SOURCE);
        final ShippingInformation shippingInformation =
                ShippingInformation.fromJson(jsonObject.optJSONObject(FIELD_SHIPPING));
        final JSONObject sourcesJson = jsonObject.optJSONObject(FIELD_SOURCES);
        
        final Boolean hasMore;
        final Integer totalCount;
        final String url;
        final List<CustomerSource> sources = new ArrayList<>();
        if (sourcesJson != null && VALUE_LIST.equals(optString(sourcesJson, FIELD_OBJECT))) {
            hasMore = optBoolean(sourcesJson, FIELD_HAS_MORE);
            totalCount = optInteger(sourcesJson, FIELD_TOTAL_COUNT);
            url = optString(sourcesJson, FIELD_URL);

            final JSONArray dataArray = sourcesJson.optJSONArray(FIELD_DATA);
            if (dataArray != null) {
                for (int i = 0; i < dataArray.length(); i++) {
                    try {
                        JSONObject customerSourceObject = dataArray.getJSONObject(i);
                        CustomerSource sourceData = CustomerSource.fromJson(customerSourceObject);
                        if (sourceData == null ||
                                VALUE_APPLE_PAY.equals(sourceData.getTokenizationMethod())) {
                            continue;
                        }
                        sources.add(sourceData);
                    } catch (JSONException ignored) {
                    }
                }
            }
        } else {
            hasMore = null;
            totalCount = null;
            url = null;
        }

        return new Customer(id, defaultSource, shippingInformation, sources, hasMore, totalCount,
                url);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return this == obj || (obj instanceof Customer && typedEquals((Customer) obj));
    }
    
    private boolean typedEquals(@NonNull Customer customer) {
        return Objects.equals(mId, customer.mId)
                && Objects.equals(mDefaultSource, customer.mDefaultSource)
                && Objects.equals(mShippingInformation, customer.mShippingInformation)
                && Objects.equals(mSources, customer.mSources)
                && Objects.equals(mHasMore, customer.mHasMore)
                && Objects.equals(mTotalCount, customer.mTotalCount)
                && Objects.equals(mUrl, customer.mUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mDefaultSource, mShippingInformation, mSources, mHasMore,
                mTotalCount, mUrl);
    }
}
