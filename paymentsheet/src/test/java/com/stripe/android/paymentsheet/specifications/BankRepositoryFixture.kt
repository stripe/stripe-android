package com.stripe.android.paymentsheet.specifications

fun getBankInitializationValue() = mapOf(
    SupportedBankType.Eps to
        """
                    [
                      {
                        "value": "arzte_und_apotheker_bank",
                        "text": "Ärzte- und Apothekerbank",
                        "icon": "arzte_und_apotheker_bank"
                      }
                    ]
                    """.trimIndent().byteInputStream(),
    SupportedBankType.Ideal to
        """
                    [
                      {
                        "value": "abn_amro",
                        "icon": "abn_amro",
                        "text": "ABN Amro"
                      }
                    ]
                    """.trimIndent().byteInputStream(),
    SupportedBankType.P24 to
        """
                    [
                      {
                        "value": "bank_millennium",
                        "icon": "bank_millennium",
                        "text": "Bank Millenium"
                      }
                    ]
                    """.trimIndent().byteInputStream()
)
