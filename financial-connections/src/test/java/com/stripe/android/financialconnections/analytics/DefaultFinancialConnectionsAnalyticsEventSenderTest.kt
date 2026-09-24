package com.stripe.android.financialconnections.analytics

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.AnalyticsRequestV2
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
internal class DefaultFinancialConnectionsAnalyticsEventSenderTest {

    @Test
    fun `public event uses the analytics executor with the merchant payload and common parameters`() = runTest {
        val requests = Turbine<AnalyticsRequestV2>()
        val sender = createSender(requests)
        val manifest = ApiKeyFixtures.sessionManifest().copy(id = "fcsess_analytics")
        val event = FinancialConnectionsEvent(Name.OPEN, Metadata(), manifest.id)

        sender.send(FinancialConnectionsAnalyticsEvent.ExternalOnEventEmitted(event), manifest)

        val request = requests.awaitItem()
        assertThat(request.eventName).isEqualTo("linked_accounts.external_on_event.emitted")
        val params = request.params.jsonObject
        assertThat(params.getValue("las_id").jsonPrimitive.content).isEqualTo(manifest.id)
        assertThat(params.getValue("key").jsonPrimitive.content).isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        assertThat(params.getValue("stripe_account").jsonPrimitive.content).isEqualTo("acct_test")
        assertThat(params.getValue("navigator_language").jsonPrimitive.content).isEqualTo("en-US")
        assertThat(params.getValue("product").jsonPrimitive.content).isEqualTo(manifest.product.value)
        assertThat(params.getValue("livemode").jsonPrimitive.content).isEqualTo(manifest.livemode.toString())
        val payload = Json.parseToJsonElement(params.getValue("event_payload").jsonPrimitive.content).jsonObject
        assertThat(payload.getValue("name").jsonPrimitive.content).isEqualTo("open")
        assertThat(payload.getValue("financialConnectionsSessionId").jsonPrimitive.content).isEqualTo(manifest.id)
        requests.ensureAllEventsConsumed()
    }

    @Test
    fun `existing analytics retains its name and event parameters`() = runTest {
        val requests = Turbine<AnalyticsRequestV2>()
        val sender = createSender(requests)
        val manifest = ApiKeyFixtures.sessionManifest()

        sender.send(FinancialConnectionsAnalyticsEvent.PaneLoaded(Pane.CONSENT), manifest)

        val request = requests.awaitItem()
        assertThat(request.eventName).isEqualTo("linked_accounts.pane.loaded")
        assertThat(request.params.jsonObject.getValue("pane").jsonPrimitive.content).isEqualTo("consent")
        assertThat(request.params.jsonObject.getValue("las_id").jsonPrimitive.content).isEqualTo(manifest.id)
        requests.ensureAllEventsConsumed()
    }

    private fun createSender(requests: Turbine<AnalyticsRequestV2>) = DefaultFinancialConnectionsAnalyticsEventSender(
        configuration = FinancialConnectionsSheetConfiguration(
            financialConnectionsSessionClientSecret = ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            stripeAccountId = "acct_test",
            preCollectedConsent = null
        ),
        locale = Locale.US,
        context = ApplicationProvider.getApplicationContext<Application>(),
        requestExecutor = AnalyticsRequestV2Executor(requests::add)
    )
}
