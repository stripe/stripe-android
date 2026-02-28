package com.stripe.android.paymentsheet.example.playground.logger

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.LogMessage
import com.stripe.android.core.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class LoggerModalViewModel : ViewModel() {
    private val logger = Logger.getInstance(true)
    private val _viewState = MutableStateFlow(ViewState())
    val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private val _vibrateEffects = MutableSharedFlow<VibrateEffect>()
    val vibrateEffect: Flow<VibrateEffect> = _vibrateEffects

    init {
        viewModelScope.launch(Dispatchers.IO) {
            logger.messages.collect { message ->
                handleNewLogMessage(message)
            }
        }
    }

    fun onTagFilterChanged(tag: String) {
        _viewState.update { state ->
            state.copy(
                tagFilter = tag,
                filteredLogs = filterLogs(state.logs, tag, state.messageFilter),
            )
        }
    }

    fun onMessageFilterChanged(query: String) {
        _viewState.update { state ->
            state.copy(
                messageFilter = query,
                filteredLogs = filterLogs(state.logs, state.tagFilter, query),
            )
        }
    }

    fun handleNewLogMessage(message: LogMessage) {
        val entry = when (message) {
            is LogMessage.Debug -> LogEntry.Debug(
                tag = message.tag,
                rawMessage = message.msg,
                isJsonMessage = isValidJson(message.msg),
                id = message.id
            )
            is LogMessage.Error -> LogEntry.Error(
                tag = message.tag,
                rawMessage = message.msg,
                isJsonMessage = isValidJson(message.msg),
                id = message.id
            )
            is LogMessage.Info -> LogEntry.Info(
                tag = message.tag,
                rawMessage = message.msg,
                isJsonMessage = isValidJson(message.msg),
                id = message.id
            )
            is LogMessage.Warning -> LogEntry.Warning(
                tag = message.tag,
                rawMessage = message.msg,
                isJsonMessage = isValidJson(message.msg),
                id = message.id
            )
        }
        _viewState.update { state ->
            val newLogs = state.logs + entry
            val newTags = if (entry.tag in state.allTags) state.allTags else state.allTags + entry.tag
            state.copy(
                logs = newLogs,
                allTags = newTags,
                filteredLogs = filterLogs(newLogs, state.tagFilter, state.messageFilter),
            )
        }

        val state = _viewState.value
        val hasFilters = state.tagFilter.isNotEmpty() || state.messageFilter.isNotEmpty()
        val passesFilter = (state.tagFilter.isEmpty() || entry.tag == state.tagFilter) &&
            (state.messageFilter.isEmpty() || entry.rawMessage.contains(state.messageFilter, ignoreCase = true))
        if (hasFilters && passesFilter) {
            emitVibrateEffect()
        }
    }

    private fun emitVibrateEffect() {
        viewModelScope.launch {
            _vibrateEffects.emit(VibrateEffect)
        }
    }

    private fun filterLogs(logs: List<LogEntry>, tagFilter: String, messageFilter: String): List<LogEntry> {
        return logs
            .filter { entry ->
                (tagFilter.isEmpty() || entry.tag == tagFilter) &&
                    (messageFilter.isEmpty() || entry.rawMessage.contains(messageFilter, ignoreCase = true))
            }
            .map { entry ->
                entry.withHighlightedMessage(messageFilter)
            }
    }

    fun isValidJson(message: String): Boolean {
        return runCatching {
            JSONObject(message)
            true
        }.getOrElse {
            false
        }
    }
}

@Immutable
data class ViewState(
    val tagFilter: String = "",
    val messageFilter: String = "",
    val logs: List<LogEntry> = emptyList(),
    val allTags: List<String> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
)

private val HighlightStyle = SpanStyle(
    background = Color(0xFFFFEB3B).copy(alpha = 0.5f),
    fontWeight = FontWeight.Bold,
)

private fun highlightMatches(text: String, query: String): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var current = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        while (current < text.length) {
            val match = lowerText.indexOf(lowerQuery, current)
            if (match == -1) {
                append(text.substring(current))
                break
            }
            append(text.substring(current, match))
            withStyle(HighlightStyle) {
                append(text.substring(match, match + query.length))
            }
            current = match + query.length
        }
    }
}

@Immutable
sealed interface LogEntry {
    val id: String
    val tag: String
    val rawMessage: String
    val message: AnnotatedString
    val isJsonMessage: Boolean

    fun withHighlightedMessage(query: String): LogEntry

    @Immutable
    data class Debug(
        override val tag: String,
        override val rawMessage: String,
        override val message: AnnotatedString = AnnotatedString(rawMessage),
        override val isJsonMessage: Boolean,
        override val id: String,
    ) : LogEntry {
        override fun withHighlightedMessage(query: String) =
            copy(message = highlightMatches(rawMessage, query))
    }

    @Immutable
    data class Info(
        override val tag: String,
        override val rawMessage: String,
        override val message: AnnotatedString = AnnotatedString(rawMessage),
        override val isJsonMessage: Boolean,
        override val id: String,
    ) : LogEntry {
        override fun withHighlightedMessage(query: String) =
            copy(message = highlightMatches(rawMessage, query))
    }

    @Immutable
    data class Warning(
        override val tag: String,
        override val rawMessage: String,
        override val message: AnnotatedString = AnnotatedString(rawMessage),
        override val isJsonMessage: Boolean,
        override val id: String,
    ) : LogEntry {
        override fun withHighlightedMessage(query: String) =
            copy(message = highlightMatches(rawMessage, query))
    }

    @Immutable
    data class Error(
        override val tag: String,
        override val rawMessage: String,
        override val message: AnnotatedString = AnnotatedString(rawMessage),
        override val isJsonMessage: Boolean,
        override val id: String,
    ) : LogEntry {
        override fun withHighlightedMessage(query: String) =
            copy(message = highlightMatches(rawMessage, query))
    }
}

data object VibrateEffect
