package com.stripe.android.ui.core.forms.resources

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.forms.Delayed
import com.stripe.android.ui.core.elements.EmptyFormSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LpmRepositoryTest {
    private val lpmRepository = LpmRepository(
        ApplicationProvider.getApplicationContext<Application>().resources
    )

    @Test
    fun `Verify field not found in schema is read from disk`() {
        lpmRepository.update(
            listOf("card", "llama"),
            """
          [
            {
                "type": "llama",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("llama")).isNotNull()
        assertThat(lpmRepository.fromCode("card")).isNotNull()
    }

    @Test
    fun `Verify only expected LPMs are found in the repository`() {
        lpmRepository.update(
            listOf("llama"),
            """
          [
            {
                "type": "affirm",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
            },
            {
                "type": "llama",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("llama")).isNotNull()
        assertThat(lpmRepository.fromCode("affirm")).isNull()
    }

    @Test
    fun `Verify field not found in schema or disk is not in repo`() {
        //...
    }

    @Test
    fun `Verify no fields in the default json are ignored the lpms package should be correct`() {
        // If this test fails, check to make sure the spec's serializer is added to
        // FormItemSpecSerializer
        LpmRepository.exposedPaymentMethods.forEach { code ->
            if (!hasEmptyForm(code)) {
                assertThat(
                    lpmRepository.fromCode(code)!!.formSpec.items
                        .filter {
                            it is EmptyFormSpec && !hasEmptyForm(code)
                        }

                ).isEmpty()
            }
        }
    }

    private fun hasEmptyForm(code: String) =
        (code == "paypal" || code == "us_bank_account") &&
            lpmRepository.fromCode(code)!!.formSpec.items.size == 1 &&
            lpmRepository.fromCode(code)!!.formSpec.items.first() == EmptyFormSpec

    @Test
    fun `Verify the repository only shows card if in lpms json`() {
        assertThat(lpmRepository.fromCode("card")).isNotNull()
        lpmRepository.update(
            emptyList(),
            """
          [
            {
                "type": "affirm",
                "async": false,
                "fields": [
                  {
                    "type": "affirm_header"
                  }
                ]
              }
         ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("card")).isNull()
    }

    // TODO(michelleb): Once we have the server implemented in production we can do filtering there instead
    // of in code here.
    @Test
    fun `Verify that unknown LPMs are not shown because not listed as exposed`() {
        lpmRepository.update(
            emptyList(),
            """
              [
                {
                    "type": "affirm",
                    "async": false,
                    "fields": [
                      {
                        "type": "affirm_header"
                      }
                    ]
                  },
                {
                    "type": "unknown_lpm",
                    "async": false,
                    "fields": [
                      {
                        "type": "affirm_header"
                      }
                    ]
                  }
             ]
            """.trimIndent()
        )
        assertThat(lpmRepository.fromCode("unknown_lpm")).isNull()
    }

    @Test
    fun `Verify that payment methods hardcoded to delayed remain regardless of json`() {
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)

        lpmRepository.update(
            emptyList(),
            """
              [
                {
                    "type": "sofort",
                    "async": false,
                    "fields": []
                  }
             ]
            """.trimIndent()
        )
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)
    }
}
