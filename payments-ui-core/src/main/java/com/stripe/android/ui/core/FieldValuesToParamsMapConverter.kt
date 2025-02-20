package com.stripe.android.ui.core

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.ParameterDestination
import com.stripe.android.uicore.forms.FormFieldEntry

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
            requiresMandate: Boolean,
            allowRedisplay: PaymentMethod.AllowRedisplay? = null,
        ): PaymentMethodCreateParams {
            val fieldValuePairsForCreateParams = fieldValuePairs.filter { entry ->
                entry.key.destination == ParameterDestination.Api.Params
            }.filterNot { entry ->
                entry.key == IdentifierSpec.SaveForFutureUse || entry.key == IdentifierSpec.CardBrand
            }
            return transformToParamsMap(
                fieldValuePairsForCreateParams,
                code
            )
                .filterOutNullValues()
                .toMap()
                .run {
                    PaymentMethodCreateParams.createWithOverride(
                        code,
                        requiresMandate = requiresMandate,
                        billingDetails = createBillingDetails(fieldValuePairsForCreateParams),
                        overrideParamMap = this,
                        productUsage = setOf("PaymentSheet"),
                        allowRedisplay = allowRedisplay,
                    )
                }
        }

        private fun createBillingDetails(
            fieldValuePairs: Map<IdentifierSpec, FormFieldEntry>,
        ): PaymentMethod.BillingDetails? {
            val billingDetails = PaymentMethod.BillingDetails.Builder()

            billingDetails.setName(fieldValuePairs[IdentifierSpec.Name]?.value)
            billingDetails.setEmail(fieldValuePairs[IdentifierSpec.Email]?.value)
            billingDetails.setPhone(fieldValuePairs[IdentifierSpec.Phone]?.value)
            billingDetails.setAddress(createAddress(fieldValuePairs))

            val builtBillingDetails = billingDetails.build()
            return if (builtBillingDetails.isFilledOut()) {
                builtBillingDetails
            } else {
                null
            }
        }

        private fun createAddress(
            fieldValuePairs: Map<IdentifierSpec, FormFieldEntry>,
        ): Address {
            val address = Address.Builder()

            address.setLine1(fieldValuePairs[IdentifierSpec.Line1]?.value)
            address.setLine2(fieldValuePairs[IdentifierSpec.Line2]?.value)
            address.setCity(fieldValuePairs[IdentifierSpec.City]?.value)
            address.setState(fieldValuePairs[IdentifierSpec.State]?.value)
            address.setCountry(fieldValuePairs[IdentifierSpec.Country]?.value)
            address.setPostalCode(fieldValuePairs[IdentifierSpec.PostalCode]?.value)

            return address.build()
        }

        /**
         * This function will convert fieldValuePairs to PaymentMethodOptionsParams.
         */
        @Suppress("ReturnCount")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun transformToPaymentMethodOptionsParams(
            fieldValuePairs: Map<IdentifierSpec, FormFieldEntry>,
            code: PaymentMethodCode,
        ): PaymentMethodOptionsParams? {
            val fieldValuePairsForOptions = fieldValuePairs.filter { entry ->
                entry.key.destination == ParameterDestination.Api.Options
            }
            return when (code) {
                PaymentMethod.Type.Blik.code -> {
                    val blikCode = fieldValuePairsForOptions[IdentifierSpec.BlikCode]?.value
                    blikCode?.let {
                        PaymentMethodOptionsParams.Blik(it)
                    }
                }
                PaymentMethod.Type.Konbini.code -> {
                    val confirmationNumber = fieldValuePairsForOptions[IdentifierSpec.KonbiniConfirmationNumber]?.value
                    confirmationNumber?.let {
                        PaymentMethodOptionsParams.Konbini(confirmationNumber)
                    }
                }
                PaymentMethod.Type.WeChatPay.code -> {
                    PaymentMethodOptionsParams.WeChatPayH5
                }
                else -> {
                    null
                }
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun transformToPaymentMethodExtraParams(
            fieldValuePairs: Map<IdentifierSpec, FormFieldEntry>,
            code: PaymentMethodCode,
        ): PaymentMethodExtraParams? {
            val fieldValuePairsForExtras = fieldValuePairs.filter { entry ->
                entry.key.destination == ParameterDestination.Local.Extras
            }
            return when (code) {
                PaymentMethod.Type.BacsDebit.code -> PaymentMethodExtraParams.BacsDebit(
                    confirmed = fieldValuePairsForExtras[IdentifierSpec.BacsDebitConfirmed]?.value?.toBoolean()
                )
                PaymentMethod.Type.Card.code -> PaymentMethodExtraParams.Card(
                    setAsDefault =
                    fieldValuePairsForExtras[IdentifierSpec.SetAsDefaultPaymentMethod]?.value?.toBoolean()
                )
                PaymentMethod.Type.USBankAccount.code -> PaymentMethodExtraParams.USBankAccount(
                    setAsDefault =
                    fieldValuePairsForExtras[IdentifierSpec.SetAsDefaultPaymentMethod]?.value?.toBoolean()
                )

                else -> null
            }
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
            code: PaymentMethodCode
        ): MutableMap<String, Any?> {
            val destMap = mutableMapOf<String, Any?>()

            val formKeyValueMap = fieldValuePairs
                .filterNot { it.key.ignoreField }
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
            if (keys.isNotEmpty()) {
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
