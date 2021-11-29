package com.stripe.android.paymentsheet.forms

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.address.parseAddressesSchema
import com.stripe.android.paymentsheet.elements.BankRepository
import com.stripe.android.paymentsheet.elements.FormInternal
import com.stripe.android.paymentsheet.elements.ResourceRepository
import com.stripe.android.paymentsheet.elements.SupportedBankType
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This will render a preview of the form in IntelliJ.  It can't access resources, and
 * it must exist in src/main, not src/test.
 */
// @Preview AGP: 7.0.0 will not cause a lint error, until then it is commented out
@SuppressLint("VisibleForTests")
@Composable
internal fun FormInternalPreview() {
    val formElements = SofortForm.items
    val addressFieldElementRepository = AddressFieldElementRepository()
    val bankRepository = BankRepository()

    addressFieldElementRepository.initialize(
        mapOf(
            "ZZ" to parseAddressesSchema(ZZ_ADDRESS)!!
        )
    )

    bankRepository.initialize(
        mapOf(
            SupportedBankType.Ideal to IDEAL_BANKS,
            SupportedBankType.Eps to EPS_Banks,
            SupportedBankType.P24 to P24_BANKS
        )
    )

    FormInternal(
        MutableStateFlow(emptyList()),
        MutableStateFlow(true),
        TransformSpecToElement(
            ResourceRepository(
                bankRepository,
                addressFieldElementRepository
            ),
            FormFragmentArguments(
                SupportedPaymentMethod.Bancontact,
                showCheckbox = false,
                showCheckboxControlledFields = true,
                merchantName = "Merchant, Inc.",
                billingDetails = PaymentSheet.BillingDetails(
                    address = PaymentSheet.Address(
                        line1 = "123 Main Street",
                        line2 = null,
                        city = "San Francisco",
                        state = "CA",
                        postalCode = "94111",
                        country = "DE",
                    ),
                    email = "email",
                    name = "Jenny Rosen",
                    phone = "+18008675309"
                ),
                injectorKey = DUMMY_INJECTOR_KEY
            )
        ).transform(formElements)
    )
}

private val EPS_Banks = """
    [
  {
    "value": "arzte_und_apotheker_bank",
    "text": "Ärzte- und Apothekerbank",
    "icon": "arzte_und_apotheker_bank"
  },
  {
    "value": "austrian_anadi_bank_ag",
    "text": "Austrian Anadi Bank AG",
    "icon": "austrian_anadi_bank_ag"
  },
  {
    "value": "bank_austria",
    "text": "Bank Austria"
  }
  ]
   
""".trimIndent().byteInputStream()

private val IDEAL_BANKS = """
    [
  {
    "value": "abn_amro",
    "icon": "abn_amro",
    "text": "ABN Amro"
  },
  {
    "value": "asn_bank",
    "icon": "asn_bank",
    "text": "ASN Bank"
  }
  ]
""".trimIndent().byteInputStream()

private val P24_BANKS = """
    [
  {
    "value": "alior_bank",
    "icon": "alior_bank",
    "text": "Alior Bank"
  },
  {
    "value": "bank_millennium",
    "icon": "bank_millennium",
    "text": "Bank Millenium"
  }
  ]
""".trimIndent().byteInputStream()

private val ZZ_ADDRESS = """
    [
      {
        "type": "addressLine1",
        "required": true
      },
      {
        "type": "addressLine2",
        "required": false
      },
      {
        "type": "locality",
        "required": true,
        "schema": {
          "nameType": "city"
        }
      }
    ]
""".trimIndent().byteInputStream()
