package com.stripe.android.utils

import java.lang.reflect.Field
import java.lang.reflect.Method

internal object ClassUtils {

    /**
     * @param clazz the class to search in
     * @param whitelist the whitelist of field names
     * @param obj the target object whose field we are accessing
     * @return the value of the found field, if exists, on the target object, or null
     */
    @JvmStatic
    fun getInternalObject(
        clazz: Class<*>,
        whitelist: Set<String>,
        obj: Any
    ): Any? {
        val field = findField(clazz, whitelist) ?: return null

        try {
            return field.get(obj)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * @param clazz the class to search in
     * @param whitelist the whitelist of field names
     * @return the [Field], made accessible, if one is found, otherwise null
     */
    @JvmStatic
    fun findField(clazz: Class<*>, whitelist: Collection<String>): Field? {
        val fields = clazz.declaredFields
        for (field in fields) {
            if (whitelist.contains(field.name)) {
                field.isAccessible = true
                return field
            }
        }
        return null
    }

    /**
     * @param clazz the class to search in
     * @param whitelist the whitelist of method names
     * @return the [Method] if one is found, otherwise null
     */
    @JvmStatic
    fun findMethod(clazz: Class<*>, whitelist: Collection<String>): Method? {
        val methods = clazz.declaredMethods
        for (method in methods) {
            if (whitelist.contains(method.name)) {
                return method
            }
        }
        return null
    }
}
