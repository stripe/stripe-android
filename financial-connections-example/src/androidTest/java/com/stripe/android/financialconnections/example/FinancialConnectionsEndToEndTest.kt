package com.stripe.android.financialconnections.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.text.LinkAnnotation
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.ExternalUiTestDriver
import com.stripe.android.testing.ExternalUiTestDriver.Companion.id
import com.stripe.android.testing.ExternalUiTestDriver.Companion.text
import com.stripe.android.testing.ExternalUiTestDriver.Companion.textMatching
import com.stripe.android.testing.ExternalUiTestRule
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import java.util.UUID

/** Real-backend coverage migrated from the Financial Connections Maestro flows. */
@RunWith(AndroidJUnit4::class)
internal class FinancialConnectionsEndToEndTest {
    @get:Rule(order = 0)
    val testName = TestName()

    @get:Rule(order = 1)
    val externalUiRule = ExternalUiTestRule()

    @get:Rule(order = 2)
    val composeRule = createEmptyComposeRule()

    @Before
    fun filterEdgeEnvironment() {
        assumeTrue(
            "This scenario is not supported by the edge merchant environment",
            BuildConfig.TEST_ENVIRONMENT != "edge" || testName.methodName in EDGE_TESTS,
        )
        clearSettings()
    }

    @After
    fun clearSettings() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("FINANCIAL_CONNECTIONS_DEBUG", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun testmodeDataTestOauthInstitution() = runOAuthDataScenario(
        merchant = "testmode",
        stripeAccountId = null,
        expectedBank = "StripeBank",
    )

    @Test
    fun testmodeDataTestOauthInstitutionConnected() = runOAuthDataScenario(
        merchant = "networking",
        stripeAccountId = "acct_1PnnD9CY58qxxwvr",
        expectedBank = null,
    )

    @Test
    fun testmodePaymentIntentTestInstitution() = runScenario(
        "integration_type=Standalone&experience=FinancialConnections&flow=PaymentIntent" +
            "&financial_connections_override_native=native&merchant=testmode&permissions=payment_method" +
            "&financial_connections_test_mode=true&financial_connections_confirm_intent=true"
    ) {
        connectAccounts()
        click(id("consent_cta"))
        click(text("Test (Non-OAuth)"))
        scrollTo(text("Success")).click()
        click(text("Connect account"))
        finishSession(".*Intent Confirmed!.*")
    }

    @Test
    fun testmodePaymentIntentTestInstitutionUnplannedDowntime() = runScenario(
        "integration_type=Standalone&experience=FinancialConnections&flow=PaymentIntent" +
            "&financial_connections_override_native=native&financial_connections_test_mode=true" +
            "&merchant=testmode&permissions=payment_method"
    ) {
        connectAccounts()
        click(id("consent_cta"))
        await(text("Search"))
        scrollTo(text("Down (Unscheduled)")).click()
        click(text("Select another bank"))
        await(text("Search"))
        scrollTo(text("Down (Unscheduled)")).click()
        click(By.desc("Close icon"))
        assertThat(scrollTo(textMatching("Failed! Request-id: .*")).text).contains("Failed!")
    }

    @Test
    fun testmodeTokenManualEntry() = runScenario(
        "integration_type=Standalone&experience=FinancialConnections&flow=Token" +
            "&financial_connections_override_native=native&financial_connections_test_mode=true" +
            "&merchant=testmode&permissions=balances,payment_method"
    ) {
        connectAccounts()
        await(id("consent_cta"))
        clickManualEntryLink()
        await(text("Enter bank details"))
        enterText(id("RoutingInput"), "110000000")
        enterText(id("AccountInput"), "000123456789")
        enterText(id("ConfirmAccountInput"), "000123456789")
        scrollTo(text("Submit")).click()
        finishSession(".*Completed!.*")
    }

    @Test
    fun testmodeTokenNme() = runScenario(
        "experience=FinancialConnections&flow=Token&financial_connections_override_native=native" +
            "&merchant=networking&financial_connections_test_mode=true&permissions=payment_method" +
            "&financial_connections_confirm_intent=true"
    ) {
        enterCustomerEmail()
        connectAccounts()
        await(id("consent_cta"))
        clickManualEntryLink()
        click(text("Use test account"))
        enterText(By.textContains("555"), "6223115555")
        click(text("Save with Link"))
        finishSession(".*Completed.*")

        connectAccounts()
        click(id("consent_cta"))
        authenticateReturningUser()
        click(text("Connect account"))
        finishSession(".*Completed.*")
    }

    @Test
    fun testmodePaymentIntentTestInstitutionInstantDebits() = runScenario(
        "integration_type=Standalone&experience=InstantDebits&flow=PaymentIntent" +
            "&financial_connections_override_native=native&merchant=networking" +
            "&financial_connections_test_mode=true&permissions=transactions,payment_method" +
            "&financial_connections_confirm_intent=false"
    ) {
        enterCustomerEmail()
        connectAccounts()
        click(id("consent_cta"))
        enterText(By.textContains("555"), "6223115555")
        click(text("Continue with Link"))
        click(id("bcinst_QsDedeogZ5PA7V"))
        scrollTo(text("Success")).click()
        click(text("Connect account"))
        assertThat(await(text("Your account was connected")).text).isEqualTo("Your account was connected")
        finishSession("Session Completed!.*")

        connectAccounts()
        click(id("consent_cta"))
        authenticateReturningUser()
        click(text("Success"))
        click(text("Connect account"))
        assertThat(await(text("Your account was connected")).text).isEqualTo("Your account was connected")
        finishSession("Session Completed!.*")
    }

    @Ignore("Disabled in Maestro until BANKCON-14726 is implemented")
    @Test
    fun testmodePaymentIntentTestInstitutionNetworking() = runScenario(
        "integration_type=Standalone&experience=FinancialConnections&flow=PaymentIntent" +
            "&financial_connections_override_native=native&merchant=networking" +
            "&financial_connections_test_mode=true&permissions=transactions,payment_method" +
            "&financial_connections_confirm_intent=true"
    ) {
        enterCustomerEmail()
        connectAccounts()
        click(id("consent_cta"))
        click(text("Test (Non-OAuth)"))
        scrollTo(text("Success")).click()
        click(textMatching("Connect [Aa]ccount"))
        enterText(By.textContains("555"), "6223115555")
        click(text("Save with Link"))
        await(text("Your account was connected and saved with Link."))
        finishSession(".*Intent Confirmed!.*")

        connectAccounts()
        click(id("consent_cta"))
        await(id("existing_email-button"))
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertThat(device.hasObject(id("top-app-bar-back-button"))).isFalse()
        authenticateReturningUser()
        click(text("Success"))
        click(text("Connect account"))
        enterText(id("OTP-0"), "111111")
        await(text("Your account was connected"))
        finishSession(".*Intent Confirmed!.*")
    }

    @Ignore("Disabled in Maestro until the test institution works on edge: RUN_BANKCON_AUX-1237")
    @Test
    fun webTestmodeDataTestOauthInstitution() = runScenario(
        "experience=FinancialConnections&flow=Data&financial_connections_override_native=web" +
            "&merchant=testmode&financial_connections_test_mode=true"
    ) {
        connectAccounts()
        click(text("Agree and continue"))
        click(textMatching(".*Ownership Accounts.*"))
        click(text("Connect accounts"))
        click(text("Done"))
        assertThat(scrollTo(textMatching(".*Completed!.*")).text).contains("Completed!")
        assertThat(scrollTo(textMatching(".*StripeBank.*")).text).contains("StripeBank")
    }

    @Ignore("Livemode FinBank flow was disabled in Maestro; keep out of PR testmode runs")
    @Test
    fun livemodeDataFinbank() = runScenario(
        "integration_type=Standalone&experience=FinancialConnections&flow=Data" +
            "&financial_connections_override_native=native&merchant=live_testing" +
            "&financial_connections_test_mode=false"
    ) {
        connectAccounts()
        click(text("Agree and continue"))
        enterText(text("Search"), "finbank")
        click(id("bcinst_JFqgSnbqNULPX5"))
        enterText(text("Banking Userid "), "f_i_n")
        enterText(text("Banking Password "), "b_a_n_k")
        click(text("Submit"))
        click(text("Connect accounts"))
        await(text("Your accounts were connected"))
        finishSession(".*Completed!.*")
        assertThat(scrollTo(textMatching(".*FinBank.*")).text).contains("FinBank")
    }

    @Ignore("Livemode MX Bank flow was disabled in Maestro; keep out of PR testmode runs")
    @Test
    fun livemodeDataMxBank() = runScenario(
        "integration_type=Standalone&experience=FinancialConnections&flow=Data" +
            "&financial_connections_override_native=native&merchant=live_testing" +
            "&financial_connections_test_mode=false"
    ) {
        connectAccounts()
        click(text("Agree and continue"))
        enterText(text("Search"), "mx")
        click(id("bcinst_NKJdZEPMVNdkLg"))
        click(id("prepane_cta"))
        click(text("Authorize"))
        await(text("Your accounts were connected"))
        finishSession(".*Completed!.*")
        assertThat(scrollTo(textMatching(".*MX Bank.*")).text).contains("MX Bank")
    }

    private fun runOAuthDataScenario(merchant: String, stripeAccountId: String?, expectedBank: String?) = runScenario(
        "integration_type=Standalone&experience=FinancialConnections&flow=Data" +
            "&financial_connections_override_native=native&merchant=$merchant" +
            "&financial_connections_test_mode=true" +
            (stripeAccountId?.let { "&stripe_account_id=$it" } ?: "")
    ) {
        connectAccounts()
        click(text("Agree and continue"))
        click(id("bcinst_LLQZzmKZMjl0j0"))
        click(id("prepane_cta"))
        click(text("Connect accounts"))
        click(id("skip_cta"))
        assertThat(await(text("Your accounts were connected")).text).isEqualTo("Your accounts were connected")
        finishSession(".*Completed!.*")
        expectedBank?.let { assertThat(scrollTo(textMatching(".*$it.*")).text).contains(it) }
    }

    private fun runScenario(query: String, block: ExternalUiTestDriver.() -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("stripeconnectionsexample://playground?$query"))
            .setClass(context, FinancialConnectionsPlaygroundActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ActivityScenario.launch<FinancialConnectionsPlaygroundActivity>(intent).use {
            ExternalUiTestDriver().block()
        }
    }

    private fun ExternalUiTestDriver.connectAccounts() {
        scrollTo(id("connect_accounts")).click()
    }

    private fun ExternalUiTestDriver.enterCustomerEmail() {
        enterText(id("Customer email setting"), "android-e2e-${UUID.randomUUID()}@example.com")
    }

    private fun ExternalUiTestDriver.clickManualEntryLink() {
        await(By.textContains("Manually verify"))
        // Activate the annotated link itself, whose bounds differ from the surrounding text.
        composeRule.onNodeWithText("Manually verify", substring = true, useUnmergedTree = true)
            .performFirstLinkClick { link ->
                val uri = (link.item as? LinkAnnotation.Url)?.url?.let(Uri::parse)
                uri?.scheme == "stripe" && uri.host in setOf("manual-entry", "link-login")
            }
    }

    private fun ExternalUiTestDriver.authenticateReturningUser() {
        click(id("existing_email-button"))
        enterText(id("OTP-0"), "111111")
    }

    private fun ExternalUiTestDriver.finishSession(expectedResult: String) {
        val next = await(By.res(java.util.regex.Pattern.compile("(?:.*:id/)?(?:skip_cta|done_button)")))
        if (next.resourceName.endsWith("skip_cta")) {
            next.click()
        }
        click(id("done_button"))
        assertThat(scrollTo(textMatching(expectedResult)).text).isNotEmpty()
    }

    private companion object {
        val EDGE_TESTS = setOf(
            "testmodeDataTestOauthInstitution",
            "testmodePaymentIntentTestInstitution",
            "testmodePaymentIntentTestInstitutionUnplannedDowntime",
            "testmodePaymentIntentTestInstitutionInstantDebits",
            "testmodeTokenManualEntry",
        )
    }
}
