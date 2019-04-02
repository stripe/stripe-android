package com.stripe.android.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

public class ObjectUtils {

    public static boolean equals(@Nullable Object obj1, @Nullable Object obj2) {
        return (obj1 == obj2) || (obj1 != null && obj1.equals(obj2));
    }

    public static int hash(@NonNull Object... values) {
        return Arrays.hashCode(values);
    }
}
