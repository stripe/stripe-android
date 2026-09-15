package com.stripe.android.paymentsheet.addresselement

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.utils.TestRules
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class AddressElementTest {
    @get:Rule
    val rules = TestRules.create()

    private val page = AddressElementPage(rules.compose)

    @Test
    fun completedAddressReturnsSucceededResult() = runAddressElementTest(page) {
        present()
        page.fillCompleteAddress()
        page.clickSave()

        assertThat(results.awaitItem()).isEqualTo(expectedResult)
    }

    @Test
    fun completedAddressSurvivesActivityRecreationAndReturnsSucceededResult() = runAddressElementTest(page) {
        val activity = present()
        page.fillCompleteAddress()

        val recreatedActivity = recreate(activity)

        assertThat(recreatedActivity).isNotSameInstanceAs(activity)
        page.waitUntilVisible()
        page.assertCompleteAddress()
        page.clickSave()

        assertThat(results.awaitItem()).isEqualTo(expectedResult)
    }

    @Test
    fun invalidAddressShowsErrorAndCanBeCorrected() = runAddressElementTest(page) {
        present()
        page.clickDisabledSave()

        page.assertRequiredFieldError()
        results.expectNoEvents()

        page.fillCompleteAddress()
        page.clickSave()

        assertThat(results.awaitItem()).isEqualTo(expectedResult)
    }

    @Test
    fun closeReturnsCanceledResult() = runAddressElementTest(page) {
        present()
        page.clickClose()

        assertThat(results.awaitItem()).isEqualTo(AddressLauncherResult.Canceled())
    }

    @Test
    fun backReturnsCanceledResult() = runAddressElementTest(page) {
        present()
        Espresso.pressBack()

        assertThat(results.awaitItem()).isEqualTo(AddressLauncherResult.Canceled())
    }

    private companion object {
        val expectedResult = AddressLauncherResult.Succeeded(
            AddressDetails(
                name = "Real Name",
                address = PaymentSheet.Address(
                    city = "Boston",
                    country = "US",
                    line1 = "1234 Main St",
                    line2 = "",
                    postalCode = "12345",
                    state = "MA",
                ),
                phoneNumber = "+1",
                isCheckboxSelected = false,
            )
        )
    }
}
