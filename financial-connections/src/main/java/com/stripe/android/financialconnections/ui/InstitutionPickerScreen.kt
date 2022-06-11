@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)

package com.stripe.android.financialconnections.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.presentation.InstitutionPickerViewModel

@Composable
internal fun InstitutionPickerScreen() {
    val viewModel: InstitutionPickerViewModel = mavericksViewModel()
    val state by viewModel.collectAsState()
    InstitutionPickerContent(
        institutionsAsync = state.institutions,
        query = state.query,
        onQueryChanged = { viewModel.onQueryChanged(it) },
        onInstitutionSelected = { viewModel.onInstitutionSelected(it) }
    )
}

@Composable
private fun InstitutionPickerContent(
    institutionsAsync: Async<InstitutionResponse>,
    query: String,
    onQueryChanged: (String) -> Unit,
    onInstitutionSelected: (Institution) -> Unit
) {
    Column {
        if (institutionsAsync.complete.not()) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        Text(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            text = "Pick your bank",
            style = MaterialTheme.typography.h4
        )
        OutlinedTextField(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            value = query,
            onValueChange = onQueryChanged,
            label = { Text("Enter bank name") }
        )
        if (institutionsAsync is Fail) {
            Text(text = "Something failed: " + institutionsAsync.error.toString())
        }
        InstitutionGrid(
            institutions = institutionsAsync()?.data ?: emptyList(),
            onInstitutionSelected = onInstitutionSelected
        )
    }
}

@Composable
private fun InstitutionGrid(
    institutions: List<Institution>,
    onInstitutionSelected: (Institution) -> Unit
) {
    LazyVerticalGrid(
        cells = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 16.dp,
            end = 12.dp,
            bottom = 16.dp
        ),
        content = {
            items(institutions) { institution ->
                Card(
                    backgroundColor = Color.Magenta,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                        .height(100.dp),
                    elevation = 8.dp,
                    onClick = { onInstitutionSelected(institution) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = institution.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
@Preview
fun PreviewScreen() {
    InstitutionPickerContent(
        Success(
            InstitutionResponse(
                listOf(
                    Institution("1", "Very long institution 1"),
                    Institution("2", "Institution 2"),
                    Institution("3", "Institution 3")
                )
            )
        ),
        query = "hola",
        onQueryChanged = {},
        onInstitutionSelected = {}
    )
}
