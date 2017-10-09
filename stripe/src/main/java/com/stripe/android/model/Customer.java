package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.StripeNetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private @Nullable String mId;
    private @Nullable String mDefaultSource;
    private @Nullable ShippingInformation mShippingInformation;

    private @NonNull List<CustomerSource> mSources = new ArrayList<>();
    private @Nullable Boolean mHasMore;
    private @Nullable Integer mTotalCount;
    private @Nullable String mUrl;

    private Customer() { }

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
        Map<String, Object> mapObject = new HashMap<>();
        mapObject.put(FIELD_ID, mId);
        mapObject.put(FIELD_OBJECT, VALUE_CUSTOMER);
        mapObject.put(FIELD_DEFAULT_SOURCE, mDefaultSource);

        StripeJsonModel.putStripeJsonModelMapIfNotNull(
                mapObject,
                FIELD_SHIPPING,
                mShippingInformation);

        Map<String, Object> sourcesObject = new HashMap<>();
        sourcesObject.put(FIELD_HAS_MORE, mHasMore);
        sourcesObject.put(FIELD_TOTAL_COUNT, mTotalCount);
        sourcesObject.put(FIELD_OBJECT, VALUE_LIST);
        sourcesObject.put(FIELD_URL, mUrl);
        StripeJsonModel.putStripeJsonModelListIfNotNull(
                sourcesObject,
                FIELD_DATA,
                mSources);
        StripeNetworkUtils.removeNullAndEmptyParams(sourcesObject);

        mapObject.put(FIELD_SOURCES, sourcesObject);

        StripeNetworkUtils.removeNullAndEmptyParams(mapObject);
        return mapObject;
    }

    @Nullable
    public static Customer fromString(String jsonString) {
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
    public static Customer fromJson(JSONObject jsonObject) {
        String objectType = optString(jsonObject, FIELD_OBJECT);
        if (!VALUE_CUSTOMER.equals(objectType)) {
            return null;
        }
        Customer customer = new Customer();
        customer.mId = optString(jsonObject, FIELD_ID);
        customer.mDefaultSource = optString(jsonObject, FIELD_DEFAULT_SOURCE);
        customer.mShippingInformation =
                ShippingInformation.fromJson(jsonObject.optJSONObject(FIELD_SHIPPING));
        JSONObject sources = jsonObject.optJSONObject(FIELD_SOURCES);
        if (sources != null && VALUE_LIST.equals(optString(sources, FIELD_OBJECT))) {
            customer.mHasMore = optBoolean(sources, FIELD_HAS_MORE);
            customer.mTotalCount = optInteger(sources, FIELD_TOTAL_COUNT);
            customer.mUrl = optString(sources, FIELD_URL);
            List<CustomerSource> sourceDataList = new ArrayList<>();
            JSONArray dataArray = sources.optJSONArray(FIELD_DATA);
            for (int i = 0; i < dataArray.length(); i++) {
                try {
                    JSONObject customerSourceObject = dataArray.getJSONObject(i);
                    CustomerSource sourceData = CustomerSource.fromJson(customerSourceObject);
                    if (sourceData == null ||
                            VALUE_APPLE_PAY.equals(sourceData.getTokenizationMethod())) {
                        continue;
                    }
                    sourceDataList.add(sourceData);
                } catch (JSONException ignored) { }
            }
            customer.mSources = sourceDataList;
        }
        return customer;
    }
}
