package com.stripe.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundScenario
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.test.core.CheckoutTestDriver
import com.stripe.android.utils.TestRules
import org.junit.After
import org.junit.Before
import org.junit.Rule

internal open class BaseCheckoutTest {
    @get:Rule
    val rules = TestRules.create()

    lateinit var testDriver: CheckoutTestDriver

    @Before
    fun before() {
        testDriver = CheckoutTestDriver(rules.compose)
    }

    @After
    fun after() {
        testDriver.teardown()
    }

    /**
     * Starts from defaults rather than the persisted settings, so each test is independent of
     * what was last run on the device.
     */
    fun checkoutSettings(
        scenario: CheckoutPlaygroundScenario.Leaf,
        block: CheckoutPlaygroundSettings.() -> Unit = {},
    ): CheckoutPlaygroundSettings {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return CheckoutPlaygroundSettings.createInMemory(
            json = null,
            defaultBackendUrl = Settings(context).playgroundBackendUrl,
        ).apply {
            applyPreset(scenario.preset)
            block()
        }
    }
}

internal fun CheckoutPlaygroundScenario.Group.leaf(key: String): CheckoutPlaygroundScenario.Leaf {
    return children.filterIsInstance<CheckoutPlaygroundScenario.Leaf>().single { it.key == key }
}
