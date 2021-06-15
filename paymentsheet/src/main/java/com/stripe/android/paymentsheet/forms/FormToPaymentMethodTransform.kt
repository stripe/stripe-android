package com.stripe.android.paymentsheet.forms

/**
 * This class will transform the fields in a form into a structure as defined by a map.
 */
class FormToPaymentMethodTransform {
    /**
     * This function will put the field values as defined in the formFieldValues into a map
     * according to the mapLayout structure.
     *
     * @param: mapLayout: This is a map of keys (strings) and their values (String or another map).  This defines
     * how the resulting map should look with no values in it.
     * @param: formFieldValues: These are the fields and their values and based on the algorithm of this function
     * will be put into a map according to the mapStructure
     */
    fun transform(
        mapStructure: Map<String, Any?>,
        formFieldValues: FormFieldValues
    ): MutableMap<String, Any?> {
        val destMap = mutableMapOf<String, Any?>()
        createMap(mapStructure, destMap, formFieldValues.getMap().mapKeys { it.key.identifier })
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