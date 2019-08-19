package com.stripe.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.DisplayMetrics;

import com.stripe.android.utils.ObjectUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

final class TelemetryClientUtil {
    @NonNull
    private final DisplayMetrics mDisplayMetrics;
    @NonNull
    private final Supplier<StripeUid> mUidSupplier;
    @NonNull
    private final String mPackageName;
    @NonNull
    private final PackageManager mPackageManager;
    @NonNull
    private final String mTimeZone;

    TelemetryClientUtil(@NonNull Context context) {
        this(context.getApplicationContext(), new UidSupplier(context));
    }

    TelemetryClientUtil(@NonNull Context context, @NonNull Supplier<StripeUid> uidSupplier) {
        this(
                uidSupplier,
                context.getResources().getDisplayMetrics(),
                ObjectUtils.getOrDefault(context.getPackageName(), ""),
                context.getPackageManager(),
                getTimeZone()
        );
    }

    @VisibleForTesting
    TelemetryClientUtil(@NonNull Supplier<StripeUid> uidSupplier,
                        @NonNull DisplayMetrics displayMetrics,
                        @NonNull String packageName,
                        @NonNull PackageManager packageManager,
                        @NonNull String timeZone) {
        mDisplayMetrics = displayMetrics;
        mUidSupplier = uidSupplier;
        mPackageName = packageName;
        mPackageManager = packageManager;
        mTimeZone = timeZone;
    }

    @NonNull
    Map<String, Object> createTelemetryMap() {
        final Map<String, Object> telemetryMap = new HashMap<>(5);
        telemetryMap.put("v2", 1);
        telemetryMap.put("tag", BuildConfig.VERSION_NAME);
        telemetryMap.put("src", "android-sdk");
        telemetryMap.put("a", createFirstMap());
        telemetryMap.put("b", createSecondMap());
        return telemetryMap;
    }

    @NonNull
    private Map<String, Object> createFirstMap() {
        final Map<String, Object> firstMap = new HashMap<>(4);
        firstMap.put("c", createSingleValuePair(Locale.getDefault().toString()));
        firstMap.put("d", createSingleValuePair(getAndroidVersionString()));
        firstMap.put("f", createSingleValuePair(getScreen()));
        firstMap.put("g", createSingleValuePair(mTimeZone));
        return firstMap;
    }

    @NonNull
    private Map<String, Object> createSecondMap() {
        final Map<String, Object> secondMap = new HashMap<>(9);
        secondMap.put("d", getHashedMuid());
        secondMap.put("k", mPackageName);
        secondMap.put("o", Build.VERSION.RELEASE);
        secondMap.put("p", Build.VERSION.SDK_INT);
        secondMap.put("q", Build.MANUFACTURER);
        secondMap.put("r", Build.BRAND);
        secondMap.put("s", Build.MODEL);
        secondMap.put("t", Build.TAGS);

        final String versionName = getVersionName();
        if (versionName != null) {
            secondMap.put("l", versionName);
        }

        return secondMap;
    }

    @Nullable
    private String getVersionName() {
        if (!StripeTextUtils.isBlank(mPackageName)) {
            try {
                final PackageInfo packageInfo =
                        mPackageManager.getPackageInfo(mPackageName, 0);
                if (packageInfo != null && packageInfo.versionName != null) {
                    return packageInfo.versionName;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        return null;
    }

    @NonNull
    private static Map<String, Object> createSingleValuePair(@NonNull String value) {
        final Map<String, Object> singleItemMap = new HashMap<>();
        singleItemMap.put("v", value);
        return singleItemMap;
    }

    @NonNull
    private static String getTimeZone() {
        final int minutes =
                (int) TimeUnit.MINUTES.convert(TimeZone.getDefault().getRawOffset(),
                        TimeUnit.MILLISECONDS);
        if (minutes % 60 == 0) {
            return String.valueOf(minutes / 60);
        }

        final BigDecimal decimalValue = new BigDecimal(minutes)
                .setScale(2, BigDecimal.ROUND_HALF_EVEN);
        final BigDecimal decHours = decimalValue.divide(
                new BigDecimal(60),
                new MathContext(2))
                .setScale(2, BigDecimal.ROUND_HALF_EVEN);
        return decHours.toString();
    }

    @NonNull
    private String getScreen() {
        return String.format(
                Locale.ENGLISH,
                "%dw_%dh_%ddpi",
                mDisplayMetrics.widthPixels,
                mDisplayMetrics.heightPixels,
                mDisplayMetrics.densityDpi
        );
    }

    @NonNull
    private static String getAndroidVersionString() {
        return String.format(
                Locale.US,
                "Android %s %s %s",
                Build.VERSION.RELEASE,
                Build.VERSION.CODENAME,
                Build.VERSION.SDK_INT
        );
    }

    @NonNull
    String getHashedUid() {
        final String uid = mUidSupplier.get().value;
        if (StripeTextUtils.isBlank(uid)) {
            return "";
        }

        return ObjectUtils.getOrDefault(
                StripeTextUtils.shaHashInput(uid),
                ""
        );
    }

    @NonNull
    private String getHashedMuid() {
        return ObjectUtils.getOrDefault(
                StripeTextUtils.shaHashInput(mPackageName + getHashedUid()),
                ""
        );
    }
}
