package com.stripe.android.ui.core.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.elements.AfterpayClearpayHeaderElement.Companion.isClearpay
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class AfterpayClearpayHeaderElementTest {

    @Test
    fun `Verify label is correct`() {
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
        )

        assertThat(
            element.getLabel(ApplicationProvider.getApplicationContext<Application>().resources)
        ).isEqualTo("Buy now or pay later with <img/> <b>ⓘ</b>")
    }

    @Test
    fun `Verify infoUrl is correct`() {
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/en_US.html")
    }

    @Test
    fun `Verify infoUrl is updated as locale changes`() {
        Locale.setDefault(Locale.UK)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
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
        )

        assertThat(element.infoUrl)
            .isEqualTo("https://static.afterpay.com/modal/en_GB.html")
    }

    @Test
    fun `Verify infoUrl is localized for France`() {
        Locale.setDefault(Locale.FRANCE)
        val element = AfterpayClearpayHeaderElement(
            IdentifierSpec.Generic("test"),
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
