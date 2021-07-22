package com.stripe.android.paymentsheet.specifications

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class BankRepositoryTest {
    @Test
    fun `Correct supported bank is returned`() {
        BankRepository.init(
            mapOf(
                SupportedBankType.Eps to
                    """
                    [
                      {
                        "value": "arzte_und_apotheker_bank",
                        "text": "Ärzte- und Apothekerbank",
                        "icon": "arzte_und_apotheker_bank"
                      }
                    ]
                    """.trimIndent()
                        .byteInputStream()
            )
        )

        val listOfBanks = BankRepository.get(SupportedBankType.Eps)
        assertThat(listOfBanks.size).isEqualTo(1)

        assertThat(listOfBanks[0])
            .isEqualTo(
                DropdownItem(
                    value = "arzte_und_apotheker_bank",
                    text = "Ärzte- und Apothekerbank",
                )
            )
    }

    @Test
    fun `Verify all supported banks are successfully read`() {
        BankRepository.init(
            SupportedBankType.values().associateWith { bankType ->
                getInputStream(bankType)
            }
        )

        // If any exceptions are thrown we know something went awry
        SupportedBankType.values()
            .forEach {
                println(BankRepository.get(it))
            }
    }

    private fun getInputStream(bankType: SupportedBankType) =
        File("src/main/assets/${bankType.assetFileName}").inputStream()
}
