package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File

class LpmFormRepositoryTest {
    private val lpmFormRepository = LpmFormRepository(mock())

    @Test
    fun `Correct supported bank is returned`() {
        lpmFormRepository.initialize(
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

        val listOfBanks = lpmFormRepository.get(SupportedBankType.Eps)
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
        lpmFormRepository.initialize(
            SupportedBankType.values().associateWith { bankType ->
                getInputStream(bankType)
            }
        )

        // If any exceptions are thrown we know something went awry
        SupportedBankType.values()
            .forEach {
                println(lpmFormRepository.get(it))
            }
    }

    private fun getInputStream(bankType: SupportedBankType) =
        File("src/main/assets/${bankType.assetFileName}").inputStream()
}
