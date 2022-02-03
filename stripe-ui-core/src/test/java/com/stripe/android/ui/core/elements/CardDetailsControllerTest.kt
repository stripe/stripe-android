package com.stripe.android.ui.core.elements

import androidx.lifecycle.asLiveData
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDetailsControllerTest {
    @Test
    fun `Verify the first field in error is returned in error flow`() {
        val cardController = CardDetailsController()

        val flowValues = mutableListOf<FieldError?>()
        cardController.error.asLiveData()
            .observeForever {
                flowValues.add(it)
            }

        cardController.numberElement.controller.onValueChange("4242424242424243")
        cardController.cvcElement.controller.onValueChange("123")
        cardController.expirationDateElement.controller.onValueChange("13")

        TestUtils.idleLooper()

        Truth.assertThat(flowValues[flowValues.size - 1]?.errorMessage).isEqualTo(
            R.string.invalid_card_number
        )

        cardController.numberElement.controller.onValueChange("4242424242424242")
        TestUtils.idleLooper()

        Truth.assertThat(flowValues[flowValues.size - 1]?.errorMessage).isEqualTo(
            R.string.incomplete_expiry_date
        )
    }
}
