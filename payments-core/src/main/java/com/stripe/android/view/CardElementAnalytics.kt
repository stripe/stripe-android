package com.stripe.android.view

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.stripe.android.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.AnalyticsRequestV2Factory
import com.stripe.android.core.networking.DefaultAnalyticsRequestV2Executor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.RealAnalyticsRequestV2Storage
import com.stripe.android.networking.PaymentAnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal interface CardElementAnalytics {
    fun reportShown(context: Context)

    fun reportInteraction(context: Context)

    fun reportFormCompleted(context: Context)

    fun saveState(outState: Bundle)

    fun restoreState(savedState: Bundle)
}

internal class DefaultCardElementAnalytics internal constructor(
    private val widgetType: CardElementWidgetType,
    private val cardElementAnalyticsRequestExecutorFactory: CardElementAnalyticsRequestExecutor.Factory,
) : CardElementAnalytics {
    internal constructor(
        widgetType: CardElementWidgetType,
    ) : this(
        widgetType = widgetType,
        cardElementAnalyticsRequestExecutorFactory = DefaultCardElementAnalyticsRequestExecutor.Factory,
    )

    private var hasReportedShown: Boolean = false
    private var hasReportedInteraction: Boolean = false
    private var hasReportedFormCompleted: Boolean = false

    override fun reportShown(context: Context) {
        if (hasReportedShown) {
            return
        }
        hasReportedShown = true
        fire(
            context = context,
            event = PaymentAnalyticsEvent.MobileCardElementShown,
            params = mapOf(PARAM_WIDGET_TYPE to widgetType.analyticsValue),
        )
    }

    override fun reportInteraction(context: Context) {
        if (hasReportedInteraction) {
            return
        }
        hasReportedInteraction = true
        fire(
            context = context,
            event = PaymentAnalyticsEvent.MobileCardElementInteraction,
            params = mapOf(PARAM_WIDGET_TYPE to widgetType.analyticsValue),
        )
    }

    override fun reportFormCompleted(context: Context) {
        if (hasReportedFormCompleted) {
            return
        }
        hasReportedFormCompleted = true
        fire(
            context = context,
            event = PaymentAnalyticsEvent.MobileCardElementFormCompleted,
            params = mapOf(PARAM_WIDGET_TYPE to widgetType.analyticsValue),
        )
    }

    override fun saveState(outState: Bundle) {
        outState.putBundle(
            STATE_CARD_ELEMENT_ANALYTICS,
            bundleOf(
                KEY_HAS_REPORTED_SHOWN to hasReportedShown,
                    KEY_HAS_REPORTED_INTERACTION to hasReportedInteraction,
                KEY_HAS_REPORTED_FORM_COMPLETED to hasReportedFormCompleted,
            )
        )
    }

    override fun restoreState(savedState: Bundle) {
        val state = savedState.getBundle(STATE_CARD_ELEMENT_ANALYTICS)

        hasReportedShown = state?.getBoolean(KEY_HAS_REPORTED_SHOWN) ?: false
        hasReportedInteraction = state?.getBoolean(KEY_HAS_REPORTED_INTERACTION) ?: false
        hasReportedFormCompleted = state?.getBoolean(KEY_HAS_REPORTED_FORM_COMPLETED) ?: false
    }

    private fun fire(
        context: Context,
        event: PaymentAnalyticsEvent,
        params: Map<String, String>,
    ) {
        cardElementAnalyticsRequestExecutorFactory
            .create(context)
            .report(event, params)
    }

    internal companion object {
        const val PARAM_WIDGET_TYPE = "widget_type"

        const val STATE_CARD_ELEMENT_ANALYTICS = "stripe_card_element_analytics_state"

        const val KEY_HAS_REPORTED_SHOWN = "stripe_card_element_has_reported_shown"
        const val KEY_HAS_REPORTED_INTERACTION = "stripe_card_element_has_reported_interaction"
        const val KEY_HAS_REPORTED_FORM_COMPLETED = "stripe_card_element_has_reported_form_completed"
    }
}

internal object NoOpCardElementCardElementAnalytics : CardElementAnalytics {
    override fun reportShown(context: Context) {
        // No-op
    }

    override fun reportInteraction(context: Context) {
        // No-op
    }

    override fun reportFormCompleted(context: Context) {
        // No-op
    }

    override fun saveState(outState: Bundle) {
        // No-op
    }

    override fun restoreState(savedState: Bundle) {
        // No-op
    }
}

internal interface CardElementAnalyticsRequestExecutor {
    fun report(
        event: PaymentAnalyticsEvent,
        params: Map<String, String>,
    )

    interface Factory {
        fun create(context: Context): CardElementAnalyticsRequestExecutor
    }
}

internal class DefaultCardElementAnalyticsRequestExecutor internal constructor(
    private val analyticsFactory: AnalyticsRequestV2Factory,
    private val analyticsExecutor: AnalyticsRequestV2Executor,
    private val coroutineScope: CoroutineScope,
) : CardElementAnalyticsRequestExecutor {
    override fun report(
        event: PaymentAnalyticsEvent,
        params: Map<String, String>,
    ) {
        coroutineScope.launch {
            analyticsExecutor.enqueue(
                analyticsFactory.createRequest(event.eventName, params),
            )
        }
    }

    object Factory : CardElementAnalyticsRequestExecutor.Factory {
        override fun create(context: Context): CardElementAnalyticsRequestExecutor {
            val applicationContext = context.applicationContext
            val logger = Logger.getInstance(BuildConfig.DEBUG)

            return DefaultCardElementAnalyticsRequestExecutor(
                analyticsFactory = AnalyticsRequestV2Factory(
                    clientId = CLIENT_ID,
                    origin = ORIGIN,
                    context = applicationContext,
                ),
                analyticsExecutor = DefaultAnalyticsRequestV2Executor(
                    context = applicationContext,
                    logger = logger,
                    networkClient = DefaultStripeNetworkClient(
                        logger = logger,
                    ),
                    storage = RealAnalyticsRequestV2Storage(applicationContext),
                    isWorkManagerAvailable = { false },
                ),
                coroutineScope = CoroutineScope(Dispatchers.IO),
            )
        }

        private const val CLIENT_ID = "stripe-mobile-sdk"
        private const val ORIGIN = "stripe-mobile-sdk-android"
    }
}
