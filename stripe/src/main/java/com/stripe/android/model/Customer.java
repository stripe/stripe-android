package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.StripeNetworkUtils;
import com.stripe.android.utils.ObjectUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.stripe.android.model.StripeJsonUtils.optBoolean;
import static com.stripe.android.model.StripeJsonUtils.optInteger;
import static com.stripe.android.model.StripeJsonUtils.optString;
import static com.stripe.android.model.StripeJsonUtils.putBooleanIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putIntegerIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putObjectIfNotNull;
import static com.stripe.android.model.StripeJsonUtils.putStringIfNotNull;

/**
 * Model for a Stripe Customer object
 */
public class Customer extends StripeJsonModel {

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

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        putStringIfNotNull(jsonObject, FIELD_ID, mId);
        putStringIfNotNull(jsonObject, FIELD_OBJECT, VALUE_CUSTOMER);
        putStringIfNotNull(jsonObject, FIELD_DEFAULT_SOURCE, mDefaultSource);
        StripeJsonModel.putStripeJsonModelIfNotNull(jsonObject,
                FIELD_SHIPPING,
                mShippingInformation);
        JSONObject sourcesObject = new JSONObject();
        putStringIfNotNull(sourcesObject, FIELD_OBJECT, VALUE_LIST);
        putBooleanIfNotNull(sourcesObject, FIELD_HAS_MORE, mHasMore);
        putIntegerIfNotNull(sourcesObject, FIELD_TOTAL_COUNT, mTotalCount);
        putStripeJsonModelListIfNotNull(sourcesObject, FIELD_DATA, mSources);
        putStringIfNotNull(sourcesObject, FIELD_URL, mUrl);

        putObjectIfNotNull(jsonObject, FIELD_SOURCES, sourcesObject);

        return jsonObject;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        final AbstractMap<String, Object> map = new HashMap<>();
        map.put(FIELD_ID, mId);
        map.put(FIELD_OBJECT, VALUE_CUSTOMER);
        map.put(FIELD_DEFAULT_SOURCE, mDefaultSource);

        StripeJsonModel.putStripeJsonModelMapIfNotNull(
                map,
                FIELD_SHIPPING,
                mShippingInformation);

        final AbstractMap<String, Object> sourcesObject = new HashMap<>();
        sourcesObject.put(FIELD_HAS_MORE, mHasMore);
        sourcesObject.put(FIELD_TOTAL_COUNT, mTotalCount);
        sourcesObject.put(FIELD_OBJECT, VALUE_LIST);
        sourcesObject.put(FIELD_URL, mUrl);
        StripeJsonModel.putStripeJsonModelListIfNotNull(
                sourcesObject,
                FIELD_DATA,
                mSources);
        StripeNetworkUtils.removeNullAndEmptyParams(sourcesObject);

        map.put(FIELD_SOURCES, sourcesObject);

        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
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
            for (int i = 0; i < dataArray.length(); i++) {
                try {
                    JSONObject customerSourceObject = dataArray.getJSONObject(i);
                    CustomerSource sourceData = CustomerSource.fromJson(customerSourceObject);
                    if (sourceData == null ||
                            VALUE_APPLE_PAY.equals(sourceData.getTokenizationMethod())) {
                        continue;
                    }
                    sources.add(sourceData);
                } catch (JSONException ignored) { }
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
        return ObjectUtils.equals(mId, customer.mId)
                && ObjectUtils.equals(mDefaultSource, customer.mDefaultSource)
                && ObjectUtils.equals(mShippingInformation, customer.mShippingInformation)
                && ObjectUtils.equals(mSources, customer.mSources)
                && ObjectUtils.equals(mHasMore, customer.mHasMore)
                && ObjectUtils.equals(mTotalCount, customer.mTotalCount)
                && ObjectUtils.equals(mUrl, customer.mUrl);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mId, mDefaultSource, mShippingInformation, mSources, mHasMore,
                mTotalCount, mUrl);
    }
}
