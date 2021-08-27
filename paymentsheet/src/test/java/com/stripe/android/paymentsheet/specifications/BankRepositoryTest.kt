package com.stripe.android.paymentsheet.specifications

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.elements.BankRepository
import com.stripe.android.paymentsheet.elements.DropdownItemSpec
import com.stripe.android.paymentsheet.elements.SupportedBankType
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File

class BankRepositoryTest {
    private val bankRepository = BankRepository(mock())
    @Test
    fun `Correct supported bank is returned`() {
        bankRepository.init(
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

        val listOfBanks = bankRepository.get(SupportedBankType.Eps)
        assertThat(listOfBanks.size).isEqualTo(1)

        assertThat(listOfBanks[0])
            .isEqualTo(
                DropdownItemSpec(
                    value = "arzte_und_apotheker_bank",
                    text = "Ärzte- und Apothekerbank",
                )
            )
    }

    @Test
    fun `Verify all supported banks are successfully read`() {
        bankRepository.init(
            SupportedBankType.values().associateWith { bankType ->
                getInputStream(bankType)
            }
        )

        // If any exceptions are thrown we know something went awry
        SupportedBankType.values()
            .forEach {
                println(bankRepository.get(it))
            }
    }

    private fun getInputStream(bankType: SupportedBankType) =
        File("src/main/assets/${bankType.assetFileName}").inputStream()
}
