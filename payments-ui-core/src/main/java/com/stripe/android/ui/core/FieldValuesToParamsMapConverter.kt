package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry

/**
 * This class converts the fields in a form into a structure as defined by a map.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FieldValuesToParamsMapConverter {
    companion object {
        /**
         * This function will convert fieldValuePairs to PaymentMethodCreateParams.
         */
        fun transformToPaymentMethodCreateParams(
            fieldValuePairs: Map<IdentifierSpec, FormFieldEntry>,
            code: PaymentMethodCode,
            requiresMandate: Boolean
        ) = transformToParamsMap(
            fieldValuePairs,
            code
        )
            .filterOutNullValues()
            .toMap()
            .run {
                PaymentMethodCreateParams.createWithOverride(
                    code,
                    requiresMandate = requiresMandate,
                    overrideParamMap = this,
                    productUsage = setOf("PaymentSheet")
                )
            }

        /**
         * This function will put the field values as defined in the fieldValuePairs into a map
         * according to their keys.
         *
         * @param: formFieldValues: These are the fields and their values and based on the algorithm of this function
         * will be put into a map according to the IdentifierSpec keys.
         */
        private fun transformToParamsMap(
            fieldValuePairs: Map<IdentifierSpec, FormFieldEntry>,
            code: PaymentMethodCode,
        ): MutableMap<String, Any?> {
            val destMap = mutableMapOf<String, Any?>()

            val formKeyValueMap = fieldValuePairs
                .mapValues { entry -> entry.value.value }
                .mapKeys { it.key.v1 }

            createMap(code, destMap, formKeyValueMap)
            return destMap
        }

        /**
         * This function will take the identifier from the form field entry, separate it on
         * square braces and construct a map from it.
         *
         * For example:
         * formFieldValues = {
         *   "billing_details\[name\]": "John Smith"
         *   "billing_details\[address\]\[line1\]": "123 Main Street"
         * }
         *
         * will return a map of:
         * dest = {
         *   billing_details = {
         *      name: "John Smith"
         *      address = {
         *         line1 = "123 Main Street"
         *      }
         *   }
         * }
         */
        @Suppress("UNCHECKED_CAST")
        private fun createMap(
            code: PaymentMethodCode,
            dest: MutableMap<String, Any?>,
            formFieldKeyValues: Map<String, String?>
        ) {
            addPath(dest, listOf("type"), code)

            formFieldKeyValues.entries
                .forEach {
                    addPath(dest, getKeys(it.key), it.value)
                }
        }

        @VisibleForTesting
        internal fun addPath(map: MutableMap<String, Any?>, keys: List<String>, value: String?) {
            val key = keys[0]
            if (keys.size == 1) {
                map[key] = value
            } else {
                var mapValueOfKey = map[key] as? MutableMap<String, Any?>
                if (mapValueOfKey == null) {
                    mapValueOfKey = mutableMapOf()
                    map[key] = mapValueOfKey
                }
                addPath(mapValueOfKey, keys.subList(1, keys.size), value)
            }
        }

        @VisibleForTesting
        internal fun getKeys(string: String) =
            ("[*" + "([A-Za-z_0-9]+)" + "]*").toRegex().findAll(string)
                .map { it.groupValues }
                .flatten()
                .filterNot { it.isEmpty() }
                .toList()
    }
}

@Suppress("UNCHECKED_CAST")
private fun <K, V> Map<K, V?>.filterOutNullValues() = filterValues { it != null } as Map<K, V>
