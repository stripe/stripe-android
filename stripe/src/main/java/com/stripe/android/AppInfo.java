package com.stripe.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data for identifying your plug-in or library.
 *
 * See <a href="https://stripe.com/docs/building-plugins#setappinfo">
 *     https://stripe.com/docs/building-plugins#setappinfo</a>.
 */
public final class AppInfo {
    @NonNull private final String mName;
    @Nullable private final String mVersion;
    @Nullable private final String mUrl;
    @Nullable private final String mPartnerId;

    @NonNull
    public static AppInfo create(@NonNull String name) {
        return new AppInfo(name, null, null, null);
    }

    @NonNull
    public static AppInfo create(@NonNull String name, @NonNull String version) {
        return new AppInfo(name, version, null, null);
    }

    @NonNull
    public static AppInfo create(@NonNull String name, @NonNull String version,
                                 @NonNull String url) {
        return new AppInfo(name, version, url, null);
    }

    @NonNull
    public static AppInfo create(@NonNull String name, @NonNull String version, @NonNull String url,
                                 @NonNull String partnerId) {
        return new AppInfo(name, version, url, partnerId);
    }

    /**
     * @param name Name of your application (e.g. "MyAwesomeApp")
     * @param version Version of your application (e.g. "1.2.34")
     * @param url Website for your application (e.g. "https://myawesomeapp.info")
     * @param partnerId Your Stripe Partner ID (e.g. "pp_partner_1234")
     */
    private AppInfo(@NonNull String name, @Nullable String version, @Nullable String url,
                    @Nullable String partnerId) {
        mName = Objects.requireNonNull(name);
        mVersion = version;
        mUrl = url;
        mPartnerId = partnerId;
    }

    @NonNull
    String toUserAgent() {
        final StringBuilder str = new StringBuilder(mName);
        if (mVersion != null) {
            str.append(String.format("/%s", mVersion));
        }
        if (mUrl != null) {
            str.append(String.format(" (%s)", mUrl));
        }
        return str.toString();
    }

    @NonNull
    Map<String, String> createClientHeaders() {
        final AbstractMap<String, String> appInfo = new HashMap<>(3);
        appInfo.put("name", mName);
        appInfo.put("version", mVersion);
        appInfo.put("url", mUrl);
        appInfo.put("partner_id", mPartnerId);

        final Map<String, String> header = new HashMap<>(1);
        header.put("application", new JSONObject(appInfo).toString());
        return header;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj) || (obj instanceof AppInfo && typedEquals((AppInfo) obj));
    }

    private boolean typedEquals(@NonNull AppInfo appInfo) {
        return ObjectUtils.equals(mName, appInfo.mName) &&
                ObjectUtils.equals(mVersion, appInfo.mVersion) &&
                ObjectUtils.equals(mUrl, appInfo.mUrl) &&
                ObjectUtils.equals(mPartnerId, appInfo.mPartnerId);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.hash(mName, mVersion, mUrl, mPartnerId);
    }
}
