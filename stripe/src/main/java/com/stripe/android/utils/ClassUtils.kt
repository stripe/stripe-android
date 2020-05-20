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
        return findField(clazz, whitelist)?.let { field ->
            runCatching {
                field.get(obj)
            }.getOrNull()
        }
    }

    /**
     * @param clazz the class to search in
     * @param whitelist the whitelist of field names
     * @return the [Field], made accessible, if one is found, otherwise null
     */
    @JvmStatic
    fun findField(clazz: Class<*>, whitelist: Collection<String>): Field? {
        val fields = clazz.declaredFields

        return fields.firstOrNull {
            whitelist.contains(it.name)
        }?.also {
            it.isAccessible = true
        }
    }

    /**
     * @param clazz the class to search in
     * @param whitelist the whitelist of method names
     * @return the [Method] if one is found, otherwise null
     */
    @JvmStatic
    fun findMethod(clazz: Class<*>, whitelist: Collection<String>): Method? {
        return clazz.declaredMethods.firstOrNull {
            whitelist.contains(it.name)
        }
    }
}
