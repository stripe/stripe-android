package com.stripe.android.paymentsheet.forms

class FormData(
    val source: Map<String, Any?>,
    val elementKeys: Map<String, String?>
) {
    fun toMap(): MutableMap<String, Any?> {
        val destMap = mutableMapOf<String, Any?>()
        createMap(source, destMap, elementKeys)
        return destMap
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private fun createMap(
            source: Map<String, Any?>,
            dest: MutableMap<String, Any?>,
            elementKeys: Map<String, String?>
        ) {
            source.keys.forEach { key ->
                if (source[key] == null) {
                    dest[key] = elementKeys[key]
                } else if (source[key] is MutableMap<*, *>) {
                    val newDestMap = mutableMapOf<String, Any?>()
                    dest[key] = newDestMap
                    createMap(source[key] as MutableMap<String, Any?>, newDestMap, elementKeys)
                }
            }
        }
    }
}