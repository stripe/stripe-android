package com.stripe.android.financialconnections.lite

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestParameterInjector::class)
internal class FinancialConnectionsSheetLiteRedirectActivityTest {
    private val applicationId = ApplicationProvider.getApplicationContext<android.content.Context>().packageName
    private val returnUrl = "stripe://financial-connections-lite/$applicationId/auth_redirect"

    @Test
    fun `bank auth return resumes Lite activity with original URL`() {
        val url = "$returnUrl?code=success"
        val activity = launch(url)

        val forwarded = shadowOf(activity).nextStartedActivity
        assertThat(forwarded.component?.className)
            .isEqualTo(FinancialConnectionsSheetLiteActivity::class.java.name)
        assertThat(forwarded.dataString).isEqualTo(url)
        assertThat(forwarded.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP).isNotEqualTo(0)
    }

    @Test
    fun `explicit intent with lookalike bank auth return is rejected`(
        @TestParameter(valuesProvider = InvalidBankReturnUrlProvider::class) url: RedirectCase
    ) {
        assertThat(shadowOf(launch(url.url.replace("{app}", applicationId))).nextStartedActivity).isNull()
    }

    private fun launch(url: String): FinancialConnectionsSheetLiteRedirectActivity {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            FinancialConnectionsSheetLiteRedirectActivity::class.java
        ).setData(Uri.parse(url))
        return Robolectric.buildActivity(FinancialConnectionsSheetLiteRedirectActivity::class.java, intent)
            .setup()
            .get()
    }
}

internal object InvalidBankReturnUrlProvider : TestParameterValuesProvider() {
    override fun provideValues(context: TestParameterValuesProvider.Context?): List<RedirectCase> {
        val bankReturn = "stripe://financial-connections-lite/{app}/auth_redirect"
        return listOf(
            RedirectCase("wrong host", "stripe://other-host/{app}/auth_redirect"),
            RedirectCase("wrong application", "stripe://financial-connections-lite/other.app/auth_redirect"),
            RedirectCase("path suffix", "$bankReturn/extra"),
            RedirectCase("encoded suffix", "$bankReturn%2Fextra"),
            RedirectCase("encoded separator", "stripe://financial-connections-lite/{app}%2Fauth_redirect"),
            RedirectCase("unexpected port", "stripe://financial-connections-lite:443/{app}/auth_redirect"),
            RedirectCase("user info", "stripe://attacker@financial-connections-lite/{app}/auth_redirect"),
            RedirectCase("wrong scheme", "stripe-auth://financial-connections-lite/{app}/auth_redirect"),
            RedirectCase("unexpected fragment", "$bankReturn#unexpected"),
            RedirectCase("malformed escape", "$bankReturn%"),
            RedirectCase("query embedding", "https://attacker.example/?next=$bankReturn"),
        )
    }
}
