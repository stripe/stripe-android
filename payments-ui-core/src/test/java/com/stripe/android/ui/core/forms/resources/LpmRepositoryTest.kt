package com.stripe.android.ui.core.forms.resources

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.forms.Delayed
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LpmRepositoryTest {

    private val lpmRepository = LpmRepository(
        ApplicationProvider.getApplicationContext<Application>().resources
    )

    @Test
    fun `Verify the repository only shows card if in lpms json`() {
        assertThat(lpmRepository.getCard()).isNotNull()
        lpmRepository.initialize(
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
        """.trimIndent().byteInputStream()
        )
        assertThat(lpmRepository.getCard()).isNull()
    }

    //TODO(michelleb): Once we have the server implemented in production we can do filtering there instead
    // of in code here.
    @Test
    fun `Verify that unknown LPMs are not shown because not listed as exposed`() {
        lpmRepository.initialize(
            """
              [
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
        """.trimIndent().byteInputStream()
        )
        assertThat(lpmRepository.fromCode("unknown_lpm")).isNull()
    }

    @Test
    fun `Verify that payment methods described as async in the json have the delayed requirement set`() {
        assertThat(
            lpmRepository.getCard()?.requirement?.piRequirements
        ).doesNotContain(Delayed)

        lpmRepository.initialize(
            """
              [
                {
                    "type": "card",
                    "async": true,
                    "fields": []
                  }
             ]
        """.trimIndent().byteInputStream()
        )
        assertThat(
            lpmRepository.getCard()?.requirement?.piRequirements
        ).contains(Delayed)
    }

    @Test
    fun `Verify that payment methods hardcoded to delayed remain regardless of json`() {
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)

        lpmRepository.initialize(
            """
              [
                {
                    "type": "sofort",
                    "async": false,
                    "fields": []
                  }
             ]
        """.trimIndent().byteInputStream()
        )
        assertThat(
            lpmRepository.fromCode("sofort")?.requirement?.piRequirements
        ).contains(Delayed)
    }
}
