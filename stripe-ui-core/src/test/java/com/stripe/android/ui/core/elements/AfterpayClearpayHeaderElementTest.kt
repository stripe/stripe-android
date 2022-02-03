package com.stripe.android.ui.core.elements

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.Amount
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
            Amount(20000, "USD")
        )

        assertThat(
            element.getLabel(ApplicationProvider.getApplicationContext<Application>().resources)
        ).isEqualTo("Pay in 4 interest-free payments of $50.00 with")
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
        ).isEqualTo("Pay in 4 interest-free payments of US$50.00 with")
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
}
