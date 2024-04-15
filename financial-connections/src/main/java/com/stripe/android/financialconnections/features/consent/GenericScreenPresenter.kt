package com.stripe.android.financialconnections.features.consent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

private val json = Json {
    ignoreUnknownKeys = true
}

internal class GenericScreenPresenter {

    private val _screenState = MutableStateFlow<ScreenState?>(null)
    val screenState: StateFlow<ScreenState?> = _screenState.asStateFlow()

    fun initialize(payload: JsonElement, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val screenPayload = json.decodeFromJsonElement<ScreenPayload>(payload)

            _screenState.value = ScreenState(
                screen = screenPayload.screen,
            )
        }
    }
}
