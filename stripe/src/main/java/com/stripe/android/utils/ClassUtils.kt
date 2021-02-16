package com.stripe.android.utils

import java.lang.reflect.Field
import java.lang.reflect.Method

internal object ClassUtils {

    /**
     * @param clazz the class to search in
     * @param allowedFields allowed field names
     * @param obj the target object whose field we are accessing
     * @return the value of the found field, if exists, on the target object, or null
     */
    @JvmStatic
    fun getInternalObject(
        clazz: Class<*>,
        allowedFields: Set<String>,
        obj: Any
    ): Any? {
        return findField(clazz, allowedFields)?.let { field ->
            runCatching {
                field.get(obj)
            }.getOrNull()
        }
    }

    /**
     * @param clazz the class to search in
     * @param allowedFields allowed field names
     * @return the [Field], made accessible, if one is found, otherwise null
     */
    @JvmStatic
    fun findField(clazz: Class<*>, allowedFields: Collection<String>): Field? {
        val fields = clazz.declaredFields

        return fields.firstOrNull {
            allowedFields.contains(it.name)
        }?.also {
            it.isAccessible = true
        }
    }

    /**
     * @param clazz the class to search in
     * @param allowedMethods allowed method names
     * @return the [Method] if one is found, otherwise null
     */
    @JvmStatic
    fun findMethod(clazz: Class<*>, allowedMethods: Collection<String>): Method? {
        return clazz.declaredMethods.firstOrNull {
            allowedMethods.contains(it.name)
        }
    }
}
