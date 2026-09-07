package com.stripe.android.financialconnections.example

import android.app.UiAutomation
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
internal class FinancialConnectionsTestLabTest {
    private lateinit var ui: TestLabUi

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ui = TestLabUi(
            device = UiDevice.getInstance(instrumentation),
            uiAutomation = instrumentation.uiAutomation,
        )
        ui.prepareDevice()
    }

    @After
    fun tearDown() {
        ui.stopChrome()
    }

    @Test
    fun runFinancialConnectionsScenario() {
        val arguments = InstrumentationRegistry.getArguments()
        val scenarioIndex = arguments.getString("scenarioIndex")?.toIntOrNull()
            ?: failWithMessage("Missing integer instrumentation argument: scenarioIndex")
        val executionId = UUID.randomUUID().toString().replace("-", "").take(12)
        val email = "ftl$scenarioIndex$executionId@gmail.com"

        when (scenarioIndex) {
            0 -> oauthDataFlow(connectedAccount = true)
            1 -> oauthDataFlow(connectedAccount = false)
            2 -> instantDebits(email)
            3 -> unplannedDowntime()
            4 -> paymentIntent()
            5 -> manualEntry()
            6 -> networkedManualEntry(email)
            else -> failWithMessage("Financial Connections scenario index must be between 0 and 6")
        }
    }

    private fun oauthDataFlow(connectedAccount: Boolean) {
        val accountSuffix = if (connectedAccount) "&stripe_account_id=acct_1PnnD9CY58qxxwvr" else ""
        val merchant = if (connectedAccount) "networking" else "testmode"
        openConnections(
            "stripeconnectionsexample://playground?integration_type=Standalone" +
                "&experience=FinancialConnections&flow=Data" +
                "&financial_connections_override_native=native" +
                "&merchant=$merchant&financial_connections_test_mode=true$accountSuffix"
        )
        ui.clickText("Agree and continue")
        ui.clickResource("bcinst_LLQZzmKZMjl0j0")
        ui.clickResource("prepane_cta")
        ui.dismissChromeFirstRunIfPresent()
        ui.clickText("Connect accounts", timeoutMs = LONG_TIMEOUT_MS)
        ui.clickResource("skip_cta")
        ui.waitForText("Your accounts were connected")
        ui.clickResource("done_button")
        ui.swipeUp(2)
        ui.waitForTextRegex(".*Completed!.*", timeoutMs = LONG_TIMEOUT_MS)
        if (!connectedAccount) {
            ui.waitForTextRegex(".*StripeBank.*")
        }
    }

    private fun instantDebits(email: String) {
        ui.launch(
            "stripeconnectionsexample://playground?integration_type=Standalone" +
                "&experience=InstantDebits&flow=PaymentIntent" +
                "&financial_connections_override_native=native&merchant=networking" +
                "&financial_connections_test_mode=true&permissions=transactions,payment_method" +
                "&financial_connections_confirm_intent=false"
        )
        ui.swipeUp(2)
        ui.clickResource("Customer email setting")
        ui.typeIntoFocusedField(email)
        ui.hideKeyboard()
        ui.swipeUp(2)
        ui.clickResource("connect_accounts")
        ui.dismissChromeFirstRunIfPresent()
        ui.clickResource("consent_cta", timeoutMs = LONG_TIMEOUT_MS)
        ui.waitForTextRegex(".*555.*")
        ui.typeIntoFocusedField("6223115555")
        ui.clickText("Continue with Link")
        ui.clickResource("bcinst_QsDedeogZ5PA7V")
        ui.dismissChromeFirstRunIfPresent()
        ui.clickText("Success", timeoutMs = LONG_TIMEOUT_MS)
        ui.clickText("Connect account")
        ui.waitForText("Your account was connected")
        ui.clickResource("done_button")
        ui.swipeUp(2)
        ui.waitForTextRegex("Session Completed!.*")

        ui.clickResource("connect_accounts")
        ui.clickResource("consent_cta")
        ui.clickResource("existing_email-button")
        ui.clickResource("OTP-0")
        ui.typeIntoFocusedField("111111")
        ui.clickText("Success")
        ui.clickText("Connect account")
        ui.waitForText("Your account was connected")
        ui.clickResource("done_button")
        ui.swipeUp(2)
        ui.waitForTextRegex("Session Completed!.*")
    }

    private fun unplannedDowntime() {
        openConnections(
            "stripeconnectionsexample://playground?integration_type=Standalone" +
                "&experience=FinancialConnections&flow=PaymentIntent" +
                "&financial_connections_override_native=native&financial_connections_test_mode=true" +
                "&merchant=testmode&permissions=payment_method"
        )
        ui.clickResource("consent_cta")
        ui.waitForText("Search")
        ui.swipeUp(4)
        ui.clickText("Down (Unscheduled)")
        ui.clickText("Select another bank")
        ui.waitForText("Search")
        ui.swipeUp(4)
        ui.clickText("Down (Unscheduled)")
        ui.waitForText("Down (Unscheduled) is currently unavailable")
        ui.clickTextOrDescription("Close icon")
        ui.swipeUp(2)
        ui.waitForTextRegex("Failed! Request-id: .*")
    }

    private fun paymentIntent() {
        openConnections(
            "stripeconnectionsexample://playground?integration_type=Standalone" +
                "&experience=FinancialConnections&flow=PaymentIntent" +
                "&financial_connections_override_native=native&merchant=testmode" +
                "&permissions=payment_method&financial_connections_test_mode=true" +
                "&financial_connections_confirm_intent=true"
        )
        ui.clickResource("consent_cta")
        ui.clickText("Test (Non-OAuth)")
        ui.dismissChromeFirstRunIfPresent()
        ui.clickText("Success", timeoutMs = LONG_TIMEOUT_MS)
        ui.clickText("Connect account")
        ui.clickResourceIfPresent("skip_cta")
        ui.clickResource("done_button")
        ui.swipeUp(2)
        ui.waitForTextRegex(".*Intent Confirmed!.*")
    }

    private fun manualEntry() {
        openConnections(
            "stripeconnectionsexample://playground?integration_type=Standalone" +
                "&experience=FinancialConnections&flow=Token" +
                "&financial_connections_override_native=native&financial_connections_test_mode=true" +
                "&merchant=testmode&permissions=balances,payment_method"
        )
        ui.clickText("Manually verify instead")
        ui.waitForText("Enter bank details")
        ui.replaceResourceText("RoutingInput", "110000000")
        ui.hideKeyboard()
        ui.swipeUp()
        ui.replaceResourceText("AccountInput", "000123456789")
        ui.hideKeyboard()
        ui.swipeUp()
        ui.replaceResourceText("ConfirmAccountInput", "000123456789")
        ui.clickText("Submit")
        ui.clickResourceIfPresent("skip_cta")
        ui.clickResource("done_button")
        ui.swipeUp(2)
        ui.waitForTextRegex(".*Completed.*")
    }

    private fun networkedManualEntry(email: String) {
        val uri =
            "stripeconnectionsexample://playground?experience=FinancialConnections&flow=Token" +
                "&financial_connections_override_native=native&merchant=networking" +
                "&financial_connections_test_mode=true&permissions=payment_method" +
                "&financial_connections_confirm_intent=true&email=$email"

        ui.launch(uri)
        ui.swipeUp(2)
        ui.clickResource("connect_accounts")
        ui.dismissChromeFirstRunIfPresent()
        ui.waitForResource("consent_cta", timeoutMs = LONG_TIMEOUT_MS)
        ui.tapPercent(xPercent = 50, yPercent = 93)
        ui.clickText("Use test account")
        ui.waitForTextRegex(".*555.*")
        ui.typeIntoFocusedField("6223115555")
        ui.clickText("Save with Link")
        ui.clickResource("done_button")
        ui.swipeUp(2)
        ui.waitForTextRegex(".*Completed.*")

        ui.launch(uri)
        ui.swipeUp(2)
        ui.clickResource("connect_accounts")
        ui.clickResource("consent_cta")
        ui.clickResource("existing_email-button")
        ui.clickResource("OTP-0")
        ui.typeIntoFocusedField("111111")
        ui.clickText("Connect account")
        ui.clickResource("done_button")
        ui.swipeUp(2)
        ui.waitForTextRegex(".*Completed.*")
    }

    private fun openConnections(uri: String) {
        ui.launch(uri)
        ui.swipeUp(2)
        ui.clickResource("connect_accounts")
        ui.dismissChromeFirstRunIfPresent()
        ui.waitForResource("consent_cta", timeoutMs = LONG_TIMEOUT_MS)
    }

    private companion object {
        const val LONG_TIMEOUT_MS = 60_000L
    }
}

private class TestLabUi(
    private val device: UiDevice,
    private val uiAutomation: UiAutomation,
) {
    fun prepareDevice() {
        device.wakeUp()
        device.pressHome()
        configureChrome()
    }

    fun launch(uri: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setClassName(CONNECTIONS_APP, "$CONNECTIONS_APP.FinancialConnectionsPlaygroundActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        device.waitForIdle()
    }

    fun stopChrome() {
        uiAutomation.executeShellCommand("am force-stop com.android.chrome").close()
    }

    fun dismissChromeFirstRunIfPresent() {
        repeat(3) {
            val clicked = clickClickableAncestorIfPresent(
                timeoutMs = CHROME_PROMPT_TIMEOUT_MS,
                selectors = arrayOf(
                    By.res("com.android.chrome", "signin_fre_dismiss_button"),
                    By.res("com.android.chrome", "terms_accept"),
                    By.res("com.android.chrome", "negative_button"),
                ),
            )
            if (!clicked) {
                return
            }
        }
    }

    fun waitForResource(id: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): UiObject2 {
        return waitFor(resourceSelector(id), "resource id", id, timeoutMs)
    }

    fun clickResource(id: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        clickClickableAncestor(
            selectors = arrayOf(resourceSelector(id)),
            selectorType = "resource id",
            value = id,
            timeoutMs = timeoutMs,
        )
    }

    fun clickResourceIfPresent(id: String, timeoutMs: Long = OPTIONAL_TIMEOUT_MS): Boolean {
        return clickClickableAncestorIfPresent(
            selectors = arrayOf(resourceSelector(id)),
            timeoutMs = timeoutMs,
        )
    }

    fun replaceResourceText(id: String, value: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                device.findObject(resourceSelector(id))?.let { control ->
                    control.click()
                    control.text = value
                    device.waitForIdle()
                    return
                }
            } catch (_: StaleObjectException) {
                // The hosted or Compose hierarchy changed; reacquire the field.
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        failWithUi("resource id", id)
    }

    fun waitForText(text: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): UiObject2 {
        return waitFor(By.text(text), "text", text, timeoutMs)
    }

    fun clickText(text: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        clickClickableAncestor(
            selectors = arrayOf(By.text(text)),
            selectorType = "text",
            value = text,
            timeoutMs = timeoutMs,
        )
    }

    fun clickTextOrDescription(value: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS) {
        clickNodeCenter(
            selectors = arrayOf(By.text(value), By.desc(value)),
            selectorType = "text or content description",
            value = value,
            timeoutMs = timeoutMs,
        )
    }

    fun waitForTextRegex(regex: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): UiObject2 {
        return waitFor(By.text(Pattern.compile(regex, Pattern.DOTALL)), "text regex", regex, timeoutMs)
    }

    fun typeIntoFocusedField(value: String) {
        device.executeShellCommand("input text $value")
        device.waitForIdle()
    }

    fun hideKeyboard() {
        device.pressBack()
        device.waitForIdle()
    }

    fun swipeUp(count: Int = 1) {
        repeat(count) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 4 / 5,
                device.displayWidth / 2,
                device.displayHeight / 4,
                20,
            )
            device.waitForIdle()
        }
    }

    fun tapPercent(xPercent: Int, yPercent: Int) {
        device.click(
            device.displayWidth * xPercent / 100,
            device.displayHeight * yPercent / 100,
        )
        device.waitForIdle()
    }

    private fun waitFor(
        selector: BySelector,
        selectorType: String,
        value: String,
        timeoutMs: Long,
    ): UiObject2 {
        return device.wait(Until.findObject(selector), timeoutMs)
            ?: failWithUi(selectorType, value)
    }

    private fun clickClickableAncestor(
        selectors: Array<BySelector>,
        selectorType: String,
        value: String,
        timeoutMs: Long,
    ) {
        if (!clickClickableAncestorIfPresent(selectors, timeoutMs)) {
            failWithUi("clickable ancestor for $selectorType", value)
        }
    }

    private fun clickClickableAncestorIfPresent(
        selectors: Array<BySelector>,
        timeoutMs: Long,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            selectors.forEach { selector ->
                val matchedNode = device.findObject(selector)
                var node = matchedNode
                try {
                    while (node != null && (!node.isClickable || !node.isEnabled)) {
                        node = node.parent
                    }
                    if (node != null) {
                        node.click()
                        device.waitForIdle()
                        return true
                    }
                    if (matchedNode?.isEnabled == true) {
                        val bounds = matchedNode.visibleBounds
                        device.click(bounds.centerX(), bounds.centerY())
                        device.waitForIdle()
                        return true
                    }
                } catch (_: StaleObjectException) {
                    // The hosted or Compose hierarchy changed; reacquire the control.
                }
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return false
    }

    private fun clickNodeCenter(
        selectors: Array<BySelector>,
        selectorType: String,
        value: String,
        timeoutMs: Long,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val matchedNode = selectors.firstNotNullOfOrNull { selector ->
                try {
                    device.findObject(selector)
                } catch (_: StaleObjectException) {
                    // The hosted or Compose hierarchy changed; reacquire the control.
                    null
                }
            }
            if (matchedNode?.isEnabled == true) {
                try {
                    val bounds = matchedNode.visibleBounds
                    device.click(bounds.centerX(), bounds.centerY())
                    device.waitForIdle()
                    return
                } catch (_: StaleObjectException) {
                    // The hosted or Compose hierarchy changed; reacquire the control.
                }
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        failWithUi(selectorType, value)
    }

    private fun resourceSelector(id: String): BySelector {
        return By.res(Pattern.compile(".*${Pattern.quote(id)}"))
    }

    private fun configureChrome() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val descriptors = uiAutomation.executeShellCommandRw("sh")
        ParcelFileDescriptor.AutoCloseOutputStream(descriptors[1]).bufferedWriter().use { writer ->
            writer.write(
                "echo chrome --disable-fre --no-default-browser-check " +
                    "--disable-features=Vulkan > /data/local/tmp/chrome-command-line"
            )
            writer.newLine()
        }
        ParcelFileDescriptor.AutoCloseInputStream(descriptors[0]).use { it.readBytes() }
    }

    private fun failWithUi(selectorType: String, value: String): Nothing {
        val hierarchy = runCatching {
            ByteArrayOutputStream().use { output ->
                device.dumpWindowHierarchy(output)
                output.toString(Charsets.UTF_8.name()).take(MAX_HIERARCHY_LENGTH)
            }
        }.getOrElse { error ->
            "Unavailable: ${error.message}"
        }
        fail(
            "Could not find $selectorType '$value'. " +
                "Current package: ${device.currentPackageName}. Hierarchy: $hierarchy",
        )
        error("unreachable")
    }

    private companion object {
        const val CONNECTIONS_APP = "com.stripe.android.financialconnections.example"
        const val DEFAULT_TIMEOUT_MS = 30_000L
        const val OPTIONAL_TIMEOUT_MS = 5_000L
        const val CHROME_PROMPT_TIMEOUT_MS = 3_000L
        const val MAX_HIERARCHY_LENGTH = 8_000
        const val POLL_INTERVAL_MS = 250L
    }
}

private fun failWithMessage(message: String): Nothing {
    fail(message)
    error("unreachable")
}
