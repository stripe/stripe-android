package com.stripe.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class TelemetryClientUtil {

    static Map<String, Object> createTelemetryMap(@NonNull final Context context) {
        Map<String, Object> telemetryMap = new HashMap<>();
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();
        telemetryMap.put("v2", 1);
        telemetryMap.put("tag", BuildConfig.VERSION_NAME);
        telemetryMap.put("src", "android-sdk");

        firstMap.put("c", createSingleValuePair(Locale.getDefault().toString()));
        firstMap.put("d", createSingleValuePair(getAndroidVersionString()));
        firstMap.put("f", createSingleValuePair(getScreen(context)));
        firstMap.put("g", createSingleValuePair(getTimeZoneString()));
        telemetryMap.put("a", firstMap);

        secondMap.put("d", getHashedMuid(context));
        String packageName = getPackageName(context);
        secondMap.put("k", packageName);
        secondMap.put("o", Build.VERSION.RELEASE);
        secondMap.put("p", Build.VERSION.SDK_INT);
        secondMap.put("q", Build.MANUFACTURER);
        secondMap.put("r", Build.BRAND);
        secondMap.put("s", Build.MODEL);
        secondMap.put("t", Build.TAGS);

        if (context.getPackageName() != null) {
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(packageName, 0);
                secondMap.put("l", pInfo.versionName);
            } catch (PackageManager.NameNotFoundException nameNotFound) { }
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
    private static String getScreen(@NonNull final Context context) {
        if (context.getResources() == null) {
            return "";
        }

        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        int density = context.getResources().getDisplayMetrics().densityDpi;

        return String.format(Locale.ENGLISH, "%dw_%dh_%ddpi", width, height, density);
    }

    @NonNull
    private static String getAndroidVersionString() {
        StringBuilder builder = new StringBuilder();
        final String delimiter = " ";
        builder.append("Android").append(delimiter)
                .append(Build.VERSION.RELEASE).append(delimiter)
                .append(Build.VERSION.CODENAME).append(delimiter)
                .append(Build.VERSION.SDK_INT);
        return builder.toString();
    }

    @SuppressWarnings("HardwareIds")
    @NonNull
    static String getHashedId(@NonNull final Context context) {
        String id = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        if (StripeTextUtils.isBlank(id)) {
            return "";
        }

        String hashId = StripeTextUtils.shaHashInput(id);
        return hashId == null ? "" : hashId;
    }

    @NonNull
    private static String getHashedMuid(@NonNull final Context context) {
        String guid = getHashedId(context);
        String packageName = getPackageName(context);
        String raw = packageName + guid;
        String hashed = StripeTextUtils.shaHashInput(raw);
        return hashed == null ? "" : hashed;
    }

    @NonNull
    private static String getPackageName(@NonNull final Context context) {
        if (context.getApplicationContext() == null
                || context.getApplicationContext().getPackageName() == null) {
            return "";
        }
        return context.getApplicationContext().getPackageName();
    }
}

