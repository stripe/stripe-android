package com.stripe.android.paymentsheet.forms

import com.stripe.android.paymentsheet.elements.country.CountryUtils
import com.stripe.android.paymentsheet.specification.FormElementSpec.SectionSpec.SectionFieldSpec.*
import java.util.Locale

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

        // Need to convert Country Fields to a country code to put it in the parameter map
        val formKeyValueMap = formFieldValues.fieldValuePairs
            .mapValues { entry ->
                if (entry.key == Country) {
                    entry.value?.let {
                        CountryUtils.getCountryCodeByName(it, Locale.getDefault())?.value
                    }
                } else {
                    entry.value
                }
            }
            .mapKeys { it.key.identifier }

        createMap(mapStructure, destMap, formKeyValueMap)
        return destMap
    }

    companion object {
        /**
         * This function will looks for each of the keys in the mapStructure and
         * if the formField contains a key that matches it will populate the value.
         *
         * For example:
         * mapStructure = {
         *   "name": null
         *   billing = {
         *      address = {
         *         "name": null
         *      }
         *   }
         * }
         * formFieldValues = {
         *   "name": "John Smith"
         * }
         *
         * will return a map of:
         * dest = {
         *   "name": "John Smith"
         *   billing = {
         *      address = {
         *         "name": "John Smith"
         *      }
         *   }
         * }
         */
        @Suppress("UNCHECKED_CAST")
        private fun createMap(
            mapStructure: Map<String, Any?>,
            dest: MutableMap<String, Any?>,
            formFieldKeyValues: Map<String, String?>
        ) {
            mapStructure.keys.forEach { key ->
                if (mapStructure[key] == null) {
                    dest[key] = formFieldKeyValues[key]
                } else if (mapStructure[key] is MutableMap<*, *>) {
                    val newDestMap = mutableMapOf<String, Any?>()
                    dest[key] = newDestMap
                    createMap(
                        mapStructure[key] as MutableMap<String, Any?>,
                        newDestMap,
                        formFieldKeyValues
                    )
                }
            }
        }
    }
}