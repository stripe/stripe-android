package com.stripe.android.paymentsheet.forms

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.paymentsheet.address.AddressFieldElementRepository
import com.stripe.android.paymentsheet.address.parseAddressesSchema
import com.stripe.android.paymentsheet.paymentdatacollection.Address
import com.stripe.android.paymentsheet.paymentdatacollection.BillingDetails
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.paymentsheet.specifications.BankRepository
import com.stripe.android.paymentsheet.specifications.ResourceRepository
import com.stripe.android.paymentsheet.specifications.SupportedBankType
import com.stripe.android.paymentsheet.specifications.sofort
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This will render a preview of the form in IntelliJ.  It can't access resources, and
 * it must exist in src/main, not src/test.
 */
@Preview
@SuppressLint("VisibleForTests")
@Composable
internal fun FormInternalPreview() {
    val formElements = sofort.layout.items
    val addressFieldElementRepository = AddressFieldElementRepository()
    val bankRepository = BankRepository()

    addressFieldElementRepository.init(
        mapOf(
            "ZZ" to parseAddressesSchema(ZZ_ADDRESS)!!
        )
    )

    bankRepository.init(
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
                "Card",
                saveForFutureUseInitialVisibility = true,
                saveForFutureUseInitialValue = true,
                "Merchant, Inc.",
                BillingDetails(
                    Address(
                        "San Fransciso",
                        "US",
                        "123 Main Street",
                        null,
                        "94111",
                        "CA",
                    )
                )

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
