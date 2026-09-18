package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.di.ActivityRetainedScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@ActivityRetainedScope
internal class FinancialConnectionsEventEmitter @Inject constructor(
    private val eventContext: FinancialConnectionsEventContext,
    private val analyticsSender: FinancialConnectionsAnalyticsEventSender,
    private val logger: Logger,
    @IOContext private val workContext: CoroutineContext
) {

    @OptIn(DelicateCoroutinesApi::class)
    fun emit(name: Name, metadata: Metadata) {
        val manifest = eventContext.manifest?.takeIf { it.id.isNotBlank() } ?: return
        val event = FinancialConnectionsEvent(
            name = name,
            metadata = metadata,
            financialConnectionsSessionId = manifest.id
        )
        logger.debug("Emitting event ${event.name} with metadata ${event.metadata}")
        FinancialConnections.emitEvent(event)

        // Preserve the event's session even if the cached manifest changes before delivery.
        GlobalScope.launch(workContext) {
            runCatching {
                analyticsSender.send(FinancialConnectionsAnalyticsEvent.ExternalOnEventEmitted(event), manifest)
            }.onFailure {
                // Analytics failures are not failures of the user's connection flow.
                logger.error("Error recording public Financial Connections event", it)
            }
        }
    }
}
