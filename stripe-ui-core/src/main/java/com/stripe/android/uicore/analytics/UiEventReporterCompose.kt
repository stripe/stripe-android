package com.stripe.android.uicore.analytics

import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import com.stripe.android.core.Logger
import com.stripe.android.uicore.BuildConfig
import kotlinx.coroutines.flow.collectLatest
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalUiEventReporter = staticCompositionLocalOf<UiEventReporter> {
    EmptyUiEventReporter
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InteractionReporterElements(
    val mutableInteractionSource: MutableInteractionSource,
    val reportInteractionManually: () -> Unit
) {
    operator fun component1() = mutableInteractionSource
    operator fun component2() = reportInteractionManually
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun rememberInteractionReporter(): InteractionReporterElements {
    val reporter = LocalUiEventReporter.current

    val interactionSource = remember {
        MutableInteractionSource()
    }

    val interacted = rememberSaveable(saver = Interacted.InteractedSaver) {
        Interacted()
    }

    val reportManually = remember(reporter) {
        return@remember {
            if (!interacted.value) {
                reporter.onFieldInteracted()

                interacted.value = true
            }
        }
    }

    LaunchedEffect(interactionSource, reportManually) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Release -> reportManually()
            }
        }
    }

    return remember(interactionSource, reportManually) {
        InteractionReporterElements(
            mutableInteractionSource = interactionSource,
            reportInteractionManually = reportManually,
        )
    }
}

@Parcelize
private class Interacted(
    var value: Boolean = false
) : Parcelable {
    object InteractedSaver : Saver<Interacted, Boolean> {
        override fun restore(value: Boolean) = Interacted(value)
        override fun SaverScope.save(value: Interacted) = value.value
    }
}

private object EmptyUiEventReporter : UiEventReporter {
    private val logger: Logger
        get() = Logger.getInstance(BuildConfig.DEBUG)

    override fun onFieldInteracted() {
        logger.debug("UiEventReporter.onFieldInteracted() event not reported")
    }

    override fun onAutofillEvent(type: String) {
        logger.debug("UiEventReporter.onAutofillEvent(name = $type) event not reported")
    }
}
