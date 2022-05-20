@file:OptIn(ExperimentalFoundationApi::class)

package com.stripe.android.financialconnections.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.compose.collectAsState
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.model.InstitutionResponse

@Composable
internal fun BankPickerScreen(
    viewModel: BankPickerViewModel,
    navController: NavHostController
) {
    val state by viewModel.collectAsState()
    val institutionsAsync = state.institutions
    InstitutionContent(
        institutionsAsync = state.institutions,
        query = state.query,
        onQueryChanged = { viewModel.onQueryChanged(it) }
    )
}

@Composable
private fun InstitutionContent(
    institutionsAsync: Async<InstitutionResponse>,
    query: String,
    onQueryChanged: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            value = query,
            onValueChange = onQueryChanged,
            label = { Text("Enter bank name") }
        )
        if (institutionsAsync.complete.not()) {
            CircularProgressIndicator()
        }
        if (institutionsAsync is Fail) {
            Text(text = "Something failed: " + institutionsAsync.error.toString())
        }
        InstitutionGrid(institutions = institutionsAsync()?.data ?: emptyList())
    }
}

@Composable
fun InstitutionGrid(institutions: List<Institution>) {
    LazyColumn(
        contentPadding = PaddingValues(8.dp),
    ) {
        items(institutions, key = { it.id }) { item: Institution ->
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .animateItemPlacement(),
                backgroundColor = Color.LightGray
            ) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}