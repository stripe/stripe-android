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
import javax.inject.Inject

private val json = Json {
    ignoreUnknownKeys = true
}

// TODO: Needed?
internal class GenericScreenPresenter @Inject constructor() {

    private val _screenState = MutableStateFlow<ScreenState?>(null)
    val screenState: StateFlow<ScreenState?> = _screenState.asStateFlow()

    var onPrimaryButtonClick: () -> Unit = {}
    var onSecondaryButtonClick: () -> Unit = {}
    var onClickableTextClick: (uri: String) -> Unit = {}

    fun initialize(payload: JsonElement, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val screenPayload = json.decodeFromJsonElement<ScreenPayload>(payload)

            _screenState.value = ScreenState(
                screen = screenPayload.screen,
            )
        }
    }

    suspend fun initialize(screen: Screen) {
        _screenState.value = ScreenState(
            screen = screen,
        )
    }
}
