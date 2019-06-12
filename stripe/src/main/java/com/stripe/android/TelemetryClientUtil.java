package com.stripe.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class TelemetryClientUtil {

    @NonNull private final Context mContext;
    @NonNull private final Supplier<StripeUid> mUidSupplier;

    TelemetryClientUtil(@NonNull Context context) {
        this(context, new UidSupplier(context));
    }

    TelemetryClientUtil(@NonNull Context context, @NonNull Supplier<StripeUid> uidSupplier) {
        mContext = context.getApplicationContext();
        mUidSupplier = uidSupplier;
    }

    @NonNull
    Map<String, Object> createTelemetryMap() {
        Map<String, Object> telemetryMap = new HashMap<>();
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();
        telemetryMap.put("v2", 1);
        telemetryMap.put("tag", BuildConfig.VERSION_NAME);
        telemetryMap.put("src", "android-sdk");

        firstMap.put("c", createSingleValuePair(Locale.getDefault().toString()));
        firstMap.put("d", createSingleValuePair(getAndroidVersionString()));
        firstMap.put("f", createSingleValuePair(getScreen()));
        firstMap.put("g", createSingleValuePair(getTimeZoneString()));
        telemetryMap.put("a", firstMap);

        secondMap.put("d", getHashedMuid());
        String packageName = getPackageName();
        secondMap.put("k", packageName);
        secondMap.put("o", Build.VERSION.RELEASE);
        secondMap.put("p", Build.VERSION.SDK_INT);
        secondMap.put("q", Build.MANUFACTURER);
        secondMap.put("r", Build.BRAND);
        secondMap.put("s", Build.MODEL);
        secondMap.put("t", Build.TAGS);

        if (mContext.getPackageName() != null) {
            try {
                final PackageInfo pInfo = mContext.getPackageManager()
                        .getPackageInfo(packageName, 0);
                secondMap.put("l", pInfo.versionName);
            } catch (PackageManager.NameNotFoundException ignored) { }
        }

        telemetryMap.put("b", secondMap);
        return telemetryMap;
    }

    @NonNull
    private static Map<String, Object> createSingleValuePair(Object value) {
        Map<String, Object> singleItemMap = new HashMap<>();
        singleItemMap.put("v", value);
        return singleItemMap;
    }

    @NonNull
    private static String getTimeZoneString() {
        int minutes =
                (int) TimeUnit.MINUTES.convert(TimeZone.getDefault().getRawOffset(),
                        TimeUnit.MILLISECONDS);
        if (minutes % 60 == 0) {
            int hours = minutes / 60;
            return String.valueOf(hours);
        }

        BigDecimal decimalValue = new BigDecimal(minutes);
        decimalValue = decimalValue.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        BigDecimal decHours = decimalValue.divide(
                new BigDecimal(60),
                new MathContext(2))
                .setScale(2, BigDecimal.ROUND_HALF_EVEN);
        return decHours.toString();
    }

    @NonNull
    private String getScreen() {
        if (mContext.getResources() == null) {
            return "";
        }

        int width = mContext.getResources().getDisplayMetrics().widthPixels;
        int height = mContext.getResources().getDisplayMetrics().heightPixels;
        int density = mContext.getResources().getDisplayMetrics().densityDpi;

        return String.format(Locale.ENGLISH, "%dw_%dh_%ddpi", width, height, density);
    }

    @NonNull
    private static String getAndroidVersionString() {
        final String delimiter = " ";
        return "Android" + delimiter +
                Build.VERSION.RELEASE + delimiter +
                Build.VERSION.CODENAME + delimiter +
                Build.VERSION.SDK_INT;
    }

    @NonNull
    String getHashedId() {
        final String id = mUidSupplier.get().value;
        if (StripeTextUtils.isBlank(id)) {
            return "";
        }

        final String hashId = StripeTextUtils.shaHashInput(id);
        return hashId == null ? "" : hashId;
    }

    @NonNull
    private String getHashedMuid() {
        final String hashed = StripeTextUtils.shaHashInput(getPackageName() + getHashedId());
        return hashed == null ? "" : hashed;
    }

    @NonNull
    private String getPackageName() {
        if (mContext.getPackageName() == null) {
            return "";
        }
        return mContext.getPackageName();
    }
}

