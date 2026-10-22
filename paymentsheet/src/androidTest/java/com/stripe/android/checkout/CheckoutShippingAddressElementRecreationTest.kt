package com.stripe.android.checkout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.Turbine
import app.cash.turbine.withTurbineTimeout
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.checkouttesting.checkoutInit
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.TestApiKeys
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.replaceText
import com.stripe.android.testing.waitUntilWithIdle
import com.stripe.paymentelementtestpages.AddressElementPage
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutShippingAddressElementRecreationTest {
    private val networkRule = NetworkRule(validationTimeout = 5.seconds)

    @get:Rule
    val testRules: TestRules = TestRules.create(networkRule = networkRule)

    private val page = AddressElementPage(testRules.compose, ApplicationProvider.getApplicationContext())

    @Test
    fun savedAddressReachesSessionWithDocumentedOnCreateRegistration() = runScenario(
        registerShippingAddressElementOnCreate = true,
    )

    @Test
    fun savedAddressReachesSessionWithDeferredElementAccess() = runScenario(
        registerShippingAddressElementOnCreate = false,
    )

    private fun runScenario(registerShippingAddressElementOnCreate: Boolean) {
        networkRule.checkoutInit { response ->
            response.testBodyFromFile("checkout-session-init.json") { json ->
                json.getJSONObject("elements_session").remove("link_settings")
                json.put(
                    "shipping_address_collection",
                    JSONObject().put("allowed_countries", JSONArray().put("US")),
                )
            }
        }
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            TestApiKeys.PUBLISHABLE,
            TestApiKeys.ACCOUNT,
        )
        val controller = CheckoutController.Builder(
            application = ApplicationProvider.getApplicationContext(),
            savedStateHandle = SavedStateHandle(),
        ).resultCallback { error("This test does not confirm a payment.") }.build()

        runBlocking {
            controller.configure(
                "${DEFAULT_CHECKOUT_SESSION_ID}_secret_example",
                CheckoutController.Configuration()
                    .shippingAddressElement(ShippingAddressElement.Configuration())
                    .defaults(
                        CheckoutController.Configuration.Defaults().shippingDetails(
                            CheckoutController.Configuration.Defaults.ContactDetails()
                                .name("Real Name")
                                .address(
                                    CheckoutController.Address()
                                        .line1("1234 Main St")
                                        .city("Boston")
                                        .state("MA")
                                        .postalCode("12345")
                                        .country("US")
                                )
                        )
                    ),
            ).getOrThrow()
        }

        val createdPresenters = Turbine<CheckoutPresenter>()
        CheckoutShippingAddressElementHostActivity.fixture =
            CheckoutShippingAddressElementHostActivity.Fixture(
                controller = controller,
                registerShippingAddressElementOnCreate = registerShippingAddressElementOnCreate,
                createdPresenters = createdPresenters,
            )
        try {
            ActivityScenario.launch(CheckoutShippingAddressElementHostActivity::class.java).use { scenario ->
                try {
                    val initialPresenter = runBlocking { createdPresenters.awaitItem() }
                    assertThat(controller.session.value?.shippingAddress?.address?.city).isEqualTo("Boston")
                    scenario.onActivity { it.presentShippingAddressElement() }
                    page.waitUntilVisible()
                    page.assertName("Real Name")

                    scenario.onActivity { it.recreate() }
                    val recreatedPresenter = runBlocking {
                        withTurbineTimeout(10.seconds) { createdPresenters.awaitItem() }
                    }
                    assertThat(recreatedPresenter).isNotSameInstanceAs(initialPresenter)
                    page.waitUntilVisible()

                    testRules.compose.replaceText("City", "Cambridge")
                    page.clickSave()

                    testRules.compose.waitUntilWithIdle(
                        conditionDescription = "saved shipping address to reach controller.session after host recreation",
                    ) {
                        controller.session.value?.shippingAddress?.address?.city == "Cambridge"
                    }
                    val shipping = controller.session.value?.shippingAddress
                    assertThat(shipping?.name).isEqualTo("Real Name")
                    assertThat(shipping?.address?.line1).isEqualTo("1234 Main St")
                    assertThat(shipping?.address?.city).isEqualTo("Cambridge")
                    createdPresenters.ensureAllEventsConsumed()
                    networkRule.validate()
                } finally {
                    scenario.onActivity { controller.destroy() }
                }
            }
        } finally {
            CheckoutShippingAddressElementHostActivity.fixture = null
        }
    }
}

@OptIn(CheckoutSessionPreview::class)
class CheckoutShippingAddressElementHostActivity : ComponentActivity() {
    private lateinit var presenter: CheckoutPresenter
    private var shippingAddressElement: ShippingAddressElement? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configuration = checkNotNull(fixture)
        presenter = configuration.controller.createPresenter(this)
        if (configuration.registerShippingAddressElementOnCreate) {
            shippingAddressElement = presenter.shippingAddressElement()
        }
        configuration.createdPresenters.add(presenter)
    }

    internal fun presentShippingAddressElement() {
        (shippingAddressElement ?: presenter.shippingAddressElement()).present()
    }

    internal data class Fixture(
        val controller: CheckoutController,
        val registerShippingAddressElementOnCreate: Boolean,
        val createdPresenters: Turbine<CheckoutPresenter>,
    )

    internal companion object {
        var fixture: Fixture? = null
    }
}
