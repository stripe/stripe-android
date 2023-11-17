package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.Logger
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ErrorCode
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import kotlin.test.assertContains

class FinancialConnectionsResponseEventEmitterTest {
    private val json: Json = Json.Default
    private val logger: Logger = mock()

    private lateinit var emitter: FinancialConnectionsResponseEventEmitter

    private val liveEvents = mutableListOf<FinancialConnectionsEvent>()

    @Before
    fun setup() {
        FinancialConnections.setEventListener { liveEvents += it }
        emitter = FinancialConnectionsResponseEventEmitter(json, logger)
    }

    @Test
    fun `emmitIfPresent - institution_unavailable_planned error event`() {
        val response = StripeResponse(
            code = 400,
            body = """
            {
                "error": {
                    "extra_fields": {
                        "events_to_emit": [
                            {
                                "type": "error",
                                "error": {
                                    "error_code": "institution_unavailable_planned"
                                }
                            }
                         ]
                    }
                }
            }
            """.trimIndent()

        )

        emitter.emitIfPresent(response)

        assertContains(
            liveEvents,
            FinancialConnectionsEvent(
                name = Name.ERROR,
                metadata = Metadata(
                    errorCode = ErrorCode.INSTITUTION_UNAVAILABLE_PLANNED
                )
            )
        )
    }
}
