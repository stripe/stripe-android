package com.stripe.android.paymentsheet.paymentdatacollection

import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.forms.FormFieldValues

/**
 * This class will transform the fields in a form into a structure as defined by a map.
 */
internal class TransformToPaymentMethodCreateParams {
    /**
     * This function will convert formFieldValue to PaymentMethodCreateParams.
     */
    fun transform(
        formFieldValues: FormFieldValues,
        paramKey: Map<String, Any?>
    ) = transformToPaymentMethodCreateParamsMap(
        formFieldValues,
        paramKey
    )
        .filterOutNullValues()
        .toMap()
        .run {
            PaymentMethod.Type.fromCode(this["type"] as String)
                ?.let {
                    PaymentMethodCreateParams(
                        it,
                        overrideParamMap = this,
                        productUsage = setOf("PaymentSheet")
                    )
                }
        }

    /**
     * This function will put the field values as defined in the formFieldValues into a map
     * according to the mapLayout structure.
     *
     * @param: mapLayout: This is a map of keys (strings) and their values (String or another map).  This defines
     * how the resulting map should look with no values in it.
     * @param: formFieldValues: These are the fields and their values and based on the algorithm of this function
     * will be put into a map according to the mapStructure
     */
    private fun transformToPaymentMethodCreateParamsMap(
        formFieldValues: FormFieldValues,
        mapStructure: Map<String, Any?>,
    ): MutableMap<String, Any?> {
        val destMap = mutableMapOf<String, Any?>()

        val formKeyValueMap = formFieldValues.fieldValuePairs
            .mapValues { entry -> entry.value.value }
            .mapKeys { it.key.key }

        createMap(mapStructure, destMap, formKeyValueMap)
        return destMap
    }

    companion object {
        /**
         * This function will look for each of the keys in the mapStructure and
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
                when {
                    mapStructure[key] == null -> {
                        dest[key] = formFieldKeyValues[key]
                    }
                    mapStructure[key] is MutableMap<*, *> -> {
                        val newDestMap = mutableMapOf<String, Any?>()
                        dest[key] = newDestMap
                        createMap(
                            mapStructure[key] as MutableMap<String, Any?>,
                            newDestMap,
                            formFieldKeyValues
                        )
                    }
                    else -> {
                        dest[key] = mapStructure[key]
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <K, V> Map<K, V?>.filterOutNullValues() = filterValues { it != null } as Map<K, V>
