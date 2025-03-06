package com.stripe.android.paymentsheet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.ui.core.elements.CardBillingAddressElement
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.TextFieldState
import java.util.Calendar.OCTOBER

class CardUIViewModel(
    private val card: PaymentMethod.Card,
    private val billingDetails: PaymentMethod.BillingDetails?,
    private val addressCollectionMode: AddressCollectionMode
) : ViewModel() {
    private val dateConfig = DateConfig()
    private val cardBillingAddressElement = CardBillingAddressElement(
        identifier = IdentifierSpec.BillingAddress,
        sameAsShippingElement = null,
        shippingValuesMap = null,
        rawValuesMap = billingDetails?.address?.let {
            mapOf(
                IdentifierSpec.Line1 to it.line1,
                IdentifierSpec.Line2 to it.line2,
                IdentifierSpec.State to it.state,
                IdentifierSpec.City to it.city,
                IdentifierSpec.Country to it.country,
                IdentifierSpec.PostalCode to it.postalCode
            )
        } ?: emptyMap()
    )

    val expDate = formattedExpiryDate()

    val addressSectionElement = SectionElement.wrap(cardBillingAddressElement)

    val collectAddress = addressCollectionMode != AddressCollectionMode.Never

    val hiddenAddressElements = buildHiddenAddressElements()

    fun validateDate(text: String): TextFieldState {
        return dateConfig.determineState(text)
    }

    private fun formattedExpiryDate(): String {
        val expiryMonth = card.expiryMonth
        val expiryYear = card.expiryYear
        if (expiryMonth == null || expiryYear == null) return ""
        val formattedExpiryMonth = if (expiryMonth < OCTOBER) {
            "0$expiryMonth"
        } else {
            expiryMonth.toString()
        }

        @Suppress("MagicNumber")
        val formattedExpiryYear = expiryYear.toString().substring(2, 4)

        return "$formattedExpiryMonth$formattedExpiryYear"
    }

    private fun buildHiddenAddressElements(): Set<IdentifierSpec> {
        return when (addressCollectionMode) {
            AddressCollectionMode.Automatic -> {
                return setOf(
                    IdentifierSpec.Line1,
                    IdentifierSpec.Line2,
                    IdentifierSpec.City,
                    IdentifierSpec.State,
                )
            }
            AddressCollectionMode.Never -> emptySet()
            AddressCollectionMode.Full -> emptySet()
        }
    }

    companion object {
        fun factory(
            card: PaymentMethod.Card,
            billingDetails: PaymentMethod.BillingDetails?,
            addressCollectionMode: AddressCollectionMode
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    CardUIViewModel(
                        card = card,
                        billingDetails = billingDetails,
                        addressCollectionMode = addressCollectionMode
                    )
                }
            }
        }
    }
}