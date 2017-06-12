package com.stripe.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Size;

import com.stripe.android.util.StripeNetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.stripe.android.util.StripeJsonUtils.optBoolean;
import static com.stripe.android.util.StripeJsonUtils.optInteger;
import static com.stripe.android.util.StripeJsonUtils.optString;
import static com.stripe.android.util.StripeJsonUtils.putBooleanIfNotNull;

import static com.stripe.android.util.StripeJsonUtils.putIntegerIfNotNull;
import static com.stripe.android.util.StripeJsonUtils.putStringIfNotNull;

/**
 * Model of the "source" object list that comes back with a {@link Customer} object
 */
public class CustomerSources extends StripeJsonModel {

    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_HAS_MORE = "has_more";
    private static final String FIELD_TOTAL_COUNT = "total_count";
    private static final String FIELD_URL = "url";

    private static final String VALUE_OBJECT = "list";

    private @Nullable List<CustomerSourceData> mData;
    private @Nullable Boolean mHasMore;
    private @Nullable @Size(min = 0) Integer mTotalCount;
    private @Nullable String mUrl;

    private CustomerSources() { }

    @Nullable
    public List<CustomerSourceData> getData() {
        return mData;
    }

    @Nullable
    public Boolean getHasMore() {
        return mHasMore;
    }

    @Nullable
    @Size(min = 0)
    public Integer getTotalCount() {
        return mTotalCount;
    }

    @Nullable
    public String getUrl() {
        return mUrl;
    }

    @Nullable
    public static CustomerSources fromString(@Nullable String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            return fromJson(jsonObject);
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    public static CustomerSources fromJson(@Nullable JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        String objectString = optString(jsonObject, FIELD_OBJECT);
        if (!VALUE_OBJECT.equals(objectString)) {
            return null;
        }

        CustomerSources customerSources = new CustomerSources();
        customerSources.mHasMore = optBoolean(jsonObject, FIELD_HAS_MORE);
        customerSources.mTotalCount = optInteger(jsonObject, FIELD_TOTAL_COUNT);
        customerSources.mUrl = optString(jsonObject, FIELD_URL);

        JSONArray jsonArray = jsonObject.optJSONArray(FIELD_DATA);
        if (jsonArray != null) {
            List<CustomerSourceData> sourceDataList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    CustomerSourceData sourceData =
                            CustomerSourceData.fromJson(jsonArray.getJSONObject(i));
                    if (sourceData != null) {
                        sourceDataList.add(sourceData);
                    }
                } catch (JSONException ignored) { }
            }
            customerSources.mData = sourceDataList;
        }

        return customerSources;
    }

    @NonNull
    @Override
    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        putStringIfNotNull(object, FIELD_OBJECT, VALUE_OBJECT);
        putStringIfNotNull(object, FIELD_URL, mUrl);
        putBooleanIfNotNull(object, FIELD_HAS_MORE, mHasMore);
        putIntegerIfNotNull(object, FIELD_TOTAL_COUNT, mTotalCount);
        putStripeJsonModelListIfNotNull(object, FIELD_DATA, mData);
        return object;
    }

    @NonNull
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(FIELD_OBJECT, VALUE_OBJECT);
        map.put(FIELD_URL, mUrl);
        map.put(FIELD_HAS_MORE, mHasMore);
        map.put(FIELD_TOTAL_COUNT, mTotalCount);
        map.put(FIELD_DATA, mData);
        StripeNetworkUtils.removeNullAndEmptyParams(map);
        return map;
    }
}
