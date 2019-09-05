package com.stripe.android.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

public final class ObjectUtils {

    public static boolean equals(@Nullable Object obj1, @Nullable Object obj2) {
        return Objects.equals(obj1, obj2);
    }

    public static int hash(@Nullable Object... values) {
        return Arrays.hashCode(values);
    }

    @NonNull
    public static <T> T getOrDefault(@Nullable T obj, @NonNull T defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    @NonNull
    public static <T extends Collection> T getOrEmpty(@Nullable T obj, @NonNull T emptyValue) {
        return (obj != null && !obj.isEmpty()) ? obj : emptyValue;
    }
}
