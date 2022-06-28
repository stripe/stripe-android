package com.stripe.android.ui.core.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isClearpay
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class AfterpayClearpayHeaderElementTest {

    @Test
    fun `Verify label is correct for USD`() {
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            Amount(20000, "USD")
        )

        assertThat(
            element.getLabel(ApplicationProvider.getApplicationContext<Application>().resources)
        ).isEqualTo(
            "Pay in 4 interest-free payments of $50.00 with <img/> " +
                "<a href=\"https://static-us.afterpay.com/javascript/modal/us_rebrand_modal.html\">" +
                "<b>ⓘ</b></a>"
        )
    }

    @Test
    fun `Verify label is correct for EUR`() {
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            Amount(20000, "EUR")
        )

        assertThat(
            element.getLabel(ApplicationProvider.getApplicationContext<Application>().resources)
        ).isEqualTo(
            "Pay in 3 interest-free payments of €66.66 with <img/> " +
                "<a href=\"https://static-us.afterpay.com/javascript/modal/us_rebrand_modal.html\">" +
                "<b>ⓘ</b></a>"
        )
    }

    @Test
    fun `Verify label amount is localized`() {
        Locale.setDefault(Locale.CANADA)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            Amount(20000, "USD")
        )

        assertThat(
            element.getLabel(ApplicationProvider.getApplicationContext<Application>().resources)
        ).isEqualTo(
            "Pay in 4 interest-free payments of US$50.00 with <img/> " +
                "<a href=\"https://static-us.afterpay.com/javascript/modal/ca_rebrand_modal.html\">" +
                "<b>ⓘ</b></a>"
        )
    }

    @Test
    fun `Verify infoUrl is correct`() {
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            Amount(123, "USD")
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static-us.afterpay.com/javascript/modal/us_rebrand_modal.html")
    }

    @Test
    fun `Verify infoUrl is localized`() {
        Locale.setDefault(Locale.UK)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            Amount(123, "USD")
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static-us.afterpay.com/javascript/modal/gb_rebrand_modal.html")
    }

    @Test
    fun `Verify check if clearpay or afterpay`() {
        Locale.setDefault(Locale.UK)
        assertThat(isClearpay()).isTrue()

        Locale.setDefault(Locale.FRANCE)
        assertThat(isClearpay()).isTrue()

        Locale.setDefault(Locale.Builder().setRegion("ES").build())
        assertThat(isClearpay()).isTrue()

        Locale.setDefault(Locale.US)
        assertThat(isClearpay()).isFalse()
    }
}
