package com.stripe.android.ui.core.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isClearpay
import com.stripe.android.uicore.elements.IdentifierSpec
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
                "<a href=\"https://static.afterpay.com/modal/en_US.html\">" +
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
                "<a href=\"https://static.afterpay.com/modal/en_US.html\">" +
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
                "<a href=\"https://static.afterpay.com/modal/en_CA.html\">" +
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
            .isEqualTo("https://static.afterpay.com/modal/en_US.html")
    }

    @Test
    fun `Verify infoUrl is updated as locale changes`() {
        Locale.setDefault(Locale.UK)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            Amount(123, "USD")
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/en_GB.html")

        Locale.setDefault(Locale.FRANCE)
        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/fr_FR.html")
    }

    @Test
    fun `Verify infoUrl is localized for GB`() {
        Locale.setDefault(Locale.UK)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            Amount(123, "USD")
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/en_GB.html")
    }

    @Test
    fun `Verify infoUrl is localized for France`() {
        Locale.setDefault(Locale.FRANCE)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            Amount(123, "USD")
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/fr_FR.html")
    }

    @Test
    fun `Verify check if clearpay or afterpay`() {
        Locale.setDefault(Locale.UK)
        assertThat(isClearpay()).isTrue()

        Locale.setDefault(Locale.FRANCE)
        assertThat(isClearpay()).isTrue()

        Locale.setDefault(Locale.Builder().setRegion("ES").build())
        assertThat(isClearpay()).isTrue()

        Locale.setDefault(Locale.Builder().setRegion("IT").build())
        assertThat(isClearpay()).isTrue()

        Locale.setDefault(Locale.US)
        assertThat(isClearpay()).isFalse()
    }
}
