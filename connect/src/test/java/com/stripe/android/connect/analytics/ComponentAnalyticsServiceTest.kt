package com.stripe.android.connect.analytics

import com.stripe.android.connect.StripeEmbeddedComponent
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ComponentAnalyticsServiceTest {

    private val analyticsService: ConnectAnalyticsService = mock()

    @Test
    fun `track event emits event with common params`() {
        val componentAnalyticsService = ComponentAnalyticsService(
            analyticsService = analyticsService,
            component = StripeEmbeddedComponent.PAYOUTS,
            publishableKey = "publishableKey123",
        )
        componentAnalyticsService.merchantId = "merchantId123"

        componentAnalyticsService.track(ConnectAnalyticsEvent.ComponentCreated)
        val mapCaptor = argumentCaptor<Map<String, Any?>>()
        verify(analyticsService).track(any(), mapCaptor.capture())
        val params = mapCaptor.firstValue

        assertEquals("merchantId123", params["merchantId"])
        assertEquals(StripeEmbeddedComponent.PAYOUTS.componentName, params["component"])
        assertContains(params, "componentInstance")
        assertEquals("publishableKey123", params["publishableKey"])
    }

    @Test
    fun `track event emits still emits null common params`() {
        val componentAnalyticsService = ComponentAnalyticsService(
            analyticsService = analyticsService,
            component = StripeEmbeddedComponent.PAYOUTS,
            publishableKey = null,
        )

        componentAnalyticsService.track(ConnectAnalyticsEvent.ComponentCreated)
        val mapCaptor = argumentCaptor<Map<String, Any?>>()
        verify(analyticsService).track(any(), mapCaptor.capture())
        val params = mapCaptor.firstValue

        assertContains(params, "merchantId")
        assertNull(params["merchantId"])
        assertContains(params, "publishableKey")
        assertNull(params["publishableKey"])
    }

    @Test
    fun `track event re-uses UUID`() {
        val componentAnalyticsService = ComponentAnalyticsService(
            analyticsService = analyticsService,
            component = StripeEmbeddedComponent.PAYOUTS,
            publishableKey = null,
        )

        componentAnalyticsService.track(ConnectAnalyticsEvent.ComponentCreated)
        componentAnalyticsService.track(ConnectAnalyticsEvent.ComponentViewed(pageViewId = null))
        val mapCaptor = argumentCaptor<Map<String, Any?>>()
        verify(analyticsService, times(2)).track(any(), mapCaptor.capture())

        val emittedParams = mapCaptor.allValues
        val expectedUUID = emittedParams.first()["componentInstance"]
        emittedParams.map { params ->
            assertEquals(expectedUUID, params["componentInstance"])
        }
    }

    @Test
    fun `track event emits event with metadata nested and at the root level`() {
        val componentAnalyticsService = ComponentAnalyticsService(
            analyticsService,
            StripeEmbeddedComponent.PAYOUTS,
            "publishableKey123"
        )

        componentAnalyticsService.track(
            ConnectAnalyticsEvent.WebComponentLoaded(
                pageViewId = "pageViewId123",
                timeToLoadMs = 100L,
                perceivedTimeToLoadMs = 50L,
            )
        )
        val mapCaptor = argumentCaptor<Map<String, Any?>>()
        verify(analyticsService).track(any(), mapCaptor.capture())
        val params = mapCaptor.firstValue

        val expectedMetadata = mapOf(
            "page_view_id" to "pageViewId123",
            "time_to_load" to "0.1",
            "perceived_time_to_load" to "0.05",
        )
        assertContains(params, "event_metadata")
        assertEquals(expectedMetadata, params["event_metadata"])
        assertEquals("pageViewId123", params["page_view_id"])
        assertEquals("0.1", params["time_to_load"])
        assertEquals("0.05", params["perceived_time_to_load"])
    }

    @Test
    fun `track event does not emit metadata when none exists`() {
        val componentAnalyticsService = ComponentAnalyticsService(
            analyticsService,
            StripeEmbeddedComponent.PAYOUTS,
            "publishableKey123"
        )

        componentAnalyticsService.track(ConnectAnalyticsEvent.ComponentCreated) // no metadata
        val mapCaptor = argumentCaptor<Map<String, Any?>>()
        verify(analyticsService).track(any(), mapCaptor.capture())
        val params = mapCaptor.firstValue

        assertFalse(params.containsKey("event_metadata"))
    }
}
