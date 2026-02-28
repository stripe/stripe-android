package com.stripe.android.paymentsheet.example.playground.logger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
fun LoggerBottomSheetContent(
    viewState: ViewState,
    onTagFilterChanged: (String) -> Unit,
    onMessageFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val logs = viewState.filteredLogs
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Text(
            text = "Logs (${logs.size})",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Divider()

        // Filters
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TagDropdown(
                selectedTag = viewState.tagFilter,
                allTags = viewState.allTags,
                onTagSelected = onTagFilterChanged,
            )
            Spacer(Modifier.height(8.dp))
            MessageFilterField(
                query = viewState.messageFilter,
                onQueryChanged = onMessageFilterChanged,
            )
        }
        Divider()

        if (logs.isEmpty()) {
            Text(
                text = if (viewState.logs.isEmpty()) "No logs yet." else "No logs match filters.",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
            ) {
                items(
                    items = logs,
                    key = {
                        it.id
                    }
                ) { entry ->
                    LogEntryRow(entry)
                    Divider()
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TagDropdown(
    selectedTag: String,
    allTags: List<String>,
    onTagSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTags = remember(allTags, searchQuery) {
        if (searchQuery.isEmpty()) {
            allTags
        } else {
            allTags.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    fun selectAndClose(tag: String) {
        onTagSelected(tag)
        expanded = false
        searchQuery = ""
    }

    Box {
        TagDropdownField(
            selectedTag = selectedTag,
            onClear = { onTagSelected("") },
            onExpand = { expanded = true },
        )

        TagDropdownMenu(
            expanded = expanded,
            searchQuery = searchQuery,
            onSearchQueryChanged = { searchQuery = it },
            selectedTag = selectedTag,
            filteredTags = filteredTags,
            onDismiss = {
                expanded = false
                searchQuery = ""
            },
            onTagSelected = ::selectAndClose,
        )
    }
}

@Composable
private fun TagDropdownField(
    selectedTag: String,
    onClear: () -> Unit,
    onExpand: () -> Unit,
) {
    OutlinedTextField(
        value = selectedTag.ifEmpty { "All tags" },
        onValueChange = {},
        readOnly = true,
        singleLine = true,
        label = { Text("Tag") },
        trailingIcon = {
            Row {
                if (selectedTag.isNotEmpty()) {
                    IconButton(onClick = onClear) { Text("\u2715") }
                }
                IconButton(onClick = onExpand) { Text("\u25BC") }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpand),
    )
}

@Composable
private fun TagDropdownMenu(
    expanded: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    selectedTag: String,
    filteredTags: List<String>,
    onDismiss: () -> Unit,
    onTagSelected: (String) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(DROPDOWN_WIDTH_FRACTION)
            .heightIn(max = 300.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search tags...") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )

        DropdownMenuItem(onClick = { onTagSelected("") }) {
            Text("All tags", fontWeight = if (selectedTag.isEmpty()) FontWeight.Bold else FontWeight.Normal)
        }

        filteredTags.forEach { tag ->
            DropdownMenuItem(onClick = { onTagSelected(tag) }) {
                Text(tag, fontWeight = if (tag == selectedTag) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun MessageFilterField(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        label = { Text("Filter messages") },
        singleLine = true,
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChanged("") }) {
                    Text("\u2715")
                }
            }
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    var expanded by rememberSaveable("${entry.id}_expanded") { mutableStateOf(false) }
    var showPrettyJson by rememberSaveable("${entry.id}_show_pretty") { mutableStateOf(false) }

    val displayText = if (showPrettyJson && entry.isJsonMessage) {
        AnnotatedString(prettyPrintJson(entry.rawMessage))
    } else {
        entry.message
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LogLevelChip(entry)
            Spacer(Modifier.width(8.dp))
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(4.dp))

        if (expanded) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.body2,
                modifier = if (showPrettyJson) {
                    Modifier.horizontalScroll(rememberScrollState())
                } else {
                    Modifier
                },
            )
        } else {
            Text(
                text = displayText,
                style = MaterialTheme.typography.body2,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    text = if (expanded) "Show less" else "Show more",
                    style = MaterialTheme.typography.caption,
                )
            }
            if (entry.isJsonMessage) {
                TextButton(onClick = { showPrettyJson = !showPrettyJson }) {
                    Text(
                        text = if (showPrettyJson) "Raw" else "Pretty JSON",
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
        }
    }
}

private fun prettyPrintJson(json: String): String {
    return runCatching {
        JSONObject(json).toString(2)
    }.getOrDefault(json)
}

private object LogLevelColors {
    val Debug = Color(0xFF4CAF50)
    val Info = Color(0xFF2196F3)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
}

@Composable
private fun LogLevelChip(entry: LogEntry) {
    val (label, color) = when (entry) {
        is LogEntry.Debug -> "DEBUG" to LogLevelColors.Debug
        is LogEntry.Info -> "INFO" to LogLevelColors.Info
        is LogEntry.Warning -> "WARN" to LogLevelColors.Warning
        is LogEntry.Error -> "ERROR" to LogLevelColors.Error
    }

    Text(
        text = label,
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private const val DROPDOWN_WIDTH_FRACTION = 0.9f
