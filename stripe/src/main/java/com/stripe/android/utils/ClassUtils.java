package com.stripe.android.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ClassUtils {
    private ClassUtils() {}

    /**
     * @param clazz the class to search in
     * @param whitelist the whitelist of field names
     * @param obj the target object whose field we are accessing
     * @return  the value of the found field, if exists, on the specified {@param obj}, or null
     */
    @Nullable
    public static Object getInternalObject(@NonNull Class clazz, @NonNull Set<String> whitelist,
                                           @NonNull Object obj) {
        final Field field = findField(clazz, whitelist);
        if (field == null) {
            return null;
        }

        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * @param clazz the class to search in
     * @param whitelist the whitelist of field names
     * @return  the {@link Field}, made accessible, if one is found, otherwise null
     */
    @Nullable
    public static Field findField(@NonNull Class clazz, @NonNull Collection<String> whitelist) {
        final Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (whitelist.contains(field.getName())) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    /**
     * @param clazz the class to search in
     * @param whitelist the whitelist of method names
     * @return the {@link Method} if one is found, otherwise null
     */
    @Nullable
    public static Method findMethod(@NonNull Class clazz, @NonNull Collection<String> whitelist) {
        final Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (whitelist.contains(method.getName())) {
                return method;
            }
        }
        return null;
    }
}
