package com.stripe.android.paymentsheet.example.playground

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.uicore.R as StripeUiCoreR

@Composable
internal fun SearchSettingsField(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        modifier = modifier
            .onFocusChanged { hasFocus = it.isFocused }
            .onKeyEvent {
                if (it.key == Key.Enter) {
                    keyboardController?.hide()
                    true
                } else {
                    false
                }
            }
            .fillMaxWidth(),
        value = query,
        placeholder = if (hasFocus) {
            null
        } else {
            @Composable {
                Text(text = "Search settings")
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { keyboardController?.show() }
        ),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
            )
        },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            @Composable {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        painter = painterResource(StripeUiCoreR.drawable.stripe_ic_material_close),
                        contentDescription = null,
                    )
                }
            }
        },
        onValueChange = onQueryChanged,
    )
}

private val WordBoundaryRegex by lazy(LazyThreadSafetyMode.NONE) { "\\s+".toRegex() }

internal fun String.matchesQuery(query: String): Boolean {
    if (query.isBlank()) {
        return true
    }

    val words = trim().split(WordBoundaryRegex)
    val queryWords = query.trim().split(WordBoundaryRegex)
    return queryWords.all { queryWord ->
        words.any { word -> word.startsWith(queryWord, ignoreCase = true) }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchSettingsFieldPreview() {
    var query by remember { mutableStateOf("") }
    SearchSettingsField(
        query = query,
        onQueryChanged = { query = it },
        modifier = Modifier,
    )
}
