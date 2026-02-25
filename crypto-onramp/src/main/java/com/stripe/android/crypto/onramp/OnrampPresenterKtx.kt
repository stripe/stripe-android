package com.stripe.android.crypto.onramp

import androidx.compose.runtime.*
import androidx.annotation.VisibleForTesting
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.stripe.android.utils.rememberActivity
import java.util.UUID

const val DEFAULT_ONRAMP_INSTANCE_KEY = "DEFAULT_ONRAMP_INSTANCE_KEY"

@Composable
fun rememberOnrampPresenter(
    coordinator: OnrampCoordinator,
    callbacks: OnrampCallbacks
): OnrampCoordinator.Presenter {
    val activity = rememberActivity { "Onramp must be created in an Activity." }
    val componentActivity = activity as? androidx.activity.ComponentActivity
        ?: error("Onramp must be hosted in a ComponentActivity.")

    //val onrampCallbackIdentifier = rememberSaveable { UUID.randomUUID().toString() }
    val onrampCallbackIdentifier = DEFAULT_ONRAMP_INSTANCE_KEY

    OnrampCallbackReferences[onrampCallbackIdentifier] = callbacks.build()
    UpdateOnrampCallbacks(
        onrampCallbackIdentifier = onrampCallbackIdentifier,
        onrampCallbacks = callbacks,
    )

    DisposableEffect(onrampCallbackIdentifier) {
        onDispose {
            OnrampCallbackReferences.remove(onrampCallbackIdentifier)
        }
    }

    return remember(coordinator, activity, onrampCallbackIdentifier) {
        coordinator.createPresenter(componentActivity, onrampCallbackIdentifier)
    }
}

@Composable
fun rememberOnrampPresenter(
    presenter: OnrampCoordinator.Presenter,
    callbacks: OnrampCallbacks,
    onrampCallbackIdentifier: String,
): OnrampCoordinator.Presenter {
    UpdateOnrampCallbacks(
        onrampCallbackIdentifier = onrampCallbackIdentifier,
        onrampCallbacks = callbacks,
    )

    DisposableEffect(onrampCallbackIdentifier) {
        onDispose { OnrampCallbackReferences.remove(onrampCallbackIdentifier) }
    }

    return presenter
}

@Composable
fun onrampCallbackAttachment(
    onrampCallbackIdentifier: String,
    callbacks: OnrampCallbacks,
) {
    val built = remember(callbacks) { callbacks.build() }

    LaunchedEffect(onrampCallbackIdentifier, built) {
        OnrampCallbackReferences[onrampCallbackIdentifier] = built
    }

    DisposableEffect(onrampCallbackIdentifier) {
        onDispose {
            OnrampCallbackReferences.remove(onrampCallbackIdentifier)
        }
    }
}

internal object OnrampCallbackReferences {
    private val instanceCallbackMap = mutableMapOf<String, OnrampCallbacks.State>()

    operator fun get(key: String): OnrampCallbacks.State? {
        return instanceCallbackMap[key] ?: instanceCallbackMap.values.firstOrNull()
    }

    operator fun set(key: String, callbacks: OnrampCallbacks.State) {
        instanceCallbackMap[key] = callbacks
    }

    fun remove(key: String) {
        instanceCallbackMap.remove(key)
    }

    @VisibleForTesting
    fun clear() {
        instanceCallbackMap.clear()
    }
}

@Composable
private fun UpdateOnrampCallbacks(
    onrampCallbackIdentifier: String,
    onrampCallbacks: OnrampCallbacks,
) {
    val latestCallbacks by rememberUpdatedState(newValue = onrampCallbacks)

    LaunchedEffect(onrampCallbackIdentifier) {
        OnrampCallbackReferences[onrampCallbackIdentifier] = latestCallbacks.build()
    }
}
