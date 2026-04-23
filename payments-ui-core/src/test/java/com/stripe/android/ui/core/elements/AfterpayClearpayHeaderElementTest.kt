package com.stripe.android.ui.core.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.LocaleTestRule
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isCashappAfterpay
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isClearpay
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class AfterpayClearpayHeaderElementTest {

    @get:Rule
    val localeRule = LocaleTestRule()

    @Test
    fun `Verify label is correct`() {
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            currency = "EUR",
        )

        assertThat(
            element.getLabel(ApplicationProvider.getApplicationContext<Application>().resources)
        ).isEqualTo("Buy now or pay later with <img/> <b>ⓘ</b>")
    }

    @Test
    fun `Verify infoUrl is correct`() {
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            currency = "EUR",
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/en_US.html")
    }

    @Test
    fun `Verify infoUrl is updated as locale changes`() {
        localeRule.setTemporarily(Locale.UK)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            currency = "GBP",
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/en_GB.html")

        localeRule.setTemporarily(Locale.FRANCE)
        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/fr_FR.html")
    }

    @Test
    fun `Verify infoUrl is localized for GB`() {
        localeRule.setTemporarily(Locale.UK)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            currency = "GBP",
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/en_GB.html")
    }

    @Test
    fun `Verify infoUrl is localized for France`() {
        localeRule.setTemporarily(Locale.FRANCE)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
            currency = "EUR",
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/fr_FR.html")
    }

    @Test
    fun `Verify check if clearpay or afterpay`() {
        assertThat(isClearpay("GBP")).isTrue()
        assertThat(isClearpay("EUR")).isFalse()
    }

    @Test
    fun `Verify check if cash app afterpay`() {
        assertThat(isCashappAfterpay("GBP")).isFalse()
        assertThat(isCashappAfterpay("EUR")).isFalse()
        assertThat(isCashappAfterpay("USD")).isTrue()
    }
}
