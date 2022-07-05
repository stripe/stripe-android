@file:OptIn(ExperimentalMaterialApi::class)

package com.stripe.android.financialconnections.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.presentation.InstitutionPickerViewModel
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTextButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun InstitutionPickerScreen() {
    val viewModel: InstitutionPickerViewModel = mavericksViewModel()
    val state by viewModel.collectAsState()

    BackHandler(state.searchMode) {
        viewModel.onCancelSearchClick()
    }

    InstitutionPickerContent(
        featuredInstitutions = state.featuredInstitutions,
        searchInstitutions = state.searchInstitutions,
        searchMode = state.searchMode,
        query = state.query,
        onQueryChanged = { viewModel.onQueryChanged(it) },
        onInstitutionSelected = { viewModel.onInstitutionSelected(it) },
        onCancelSearchClick = { viewModel.onCancelSearchClick() },
        onSearchFocused = { viewModel.onSearchFocused() }
    )
}

@Composable
private fun InstitutionPickerContent(
    featuredInstitutions: Async<InstitutionResponse>,
    searchInstitutions: Async<InstitutionResponse>,
    searchMode: Boolean,
    query: String,
    onQueryChanged: (String) -> Unit,
    onInstitutionSelected: (Institution) -> Unit,
    onCancelSearchClick: () -> Unit,
    onSearchFocused: () -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = { if (!searchMode) { FinancialConnectionsTopAppBar() } }
    ) {
        Column {
            if (searchInstitutions is Loading) {
                LinearProgressIndicator(
                    color = FinancialConnectionsTheme.colors.textBrand,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                if (searchMode.not()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth(),
                        text = stringResource(R.string.stripe_institutionpicker_pane_select_bank),
                        style = FinancialConnectionsTheme.typography.subtitle
                    )
                }
                Spacer(modifier = Modifier.size(16.dp))
                FinancialConnectionsSearchRow(
                    query = query,
                    searchMode = searchMode,
                    onQueryChanged = onQueryChanged,
                    onSearchFocused = onSearchFocused,
                    onCancelSearchClick = onCancelSearchClick
                )
//                if (featuredInstitutions is Fail) {
//                    Text(text = "Something failed: " + featuredInstitutions.error.toString())
//                }
                if (query.isNotEmpty()) {
                    SearchInstitutionsList(
                        institutions = searchInstitutions()?.data ?: emptyList(),
                        onInstitutionSelected = onInstitutionSelected
                    )
                } else {
                    FeaturedInstitutionsGrid(
                        institutions = featuredInstitutions()?.data ?: emptyList(),
                        onInstitutionSelected = onInstitutionSelected
                    )
                }

            }
        }
    }
}

@Composable
private fun FinancialConnectionsSearchRow(
    query: String,
    onQueryChanged: (String) -> Unit,
    onCancelSearchClick: () -> Unit,
    onSearchFocused: () -> Unit,
    searchMode: Boolean
) {
    val focusManager = LocalFocusManager.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        FinancialConnectionsOutlinedTextField(
            modifier = Modifier
                .onFocusChanged { if (it.isFocused) onSearchFocused() }
                .weight(1f),
            value = query,
            onValueChange = onQueryChanged,
        )
        if (searchMode) {
            FinancialConnectionsTextButton(
                onClick = {
                    onCancelSearchClick()
                    focusManager.clearFocus()
                }) {
                Text(text = "Cancel")
            }
        }
    }

}

@Composable
private fun SearchInstitutionsList(
    institutions: List<Institution>,
    onInstitutionSelected: (Institution) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp),
        content = {
            items(institutions, key = { it.id }) { institution ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onInstitutionSelected(institution) }
                        .padding(vertical = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))

                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Column {
                        Text(
                            text = institution.name,
                            color = FinancialConnectionsTheme.colors.textPrimary,
                            style = FinancialConnectionsTheme.typography.bodyEmphasized,
                        )
                        Text(
                            text = institution.url ?: "",
                            color = FinancialConnectionsTheme.colors.textSecondary,
                            style = FinancialConnectionsTheme.typography.captionTight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                }
            }
        }
    )
}

@Composable
private fun FeaturedInstitutionsGrid(
    institutions: List<Institution>,
    onInstitutionSelected: (Institution) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            items(institutions) { institution ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(FinancialConnectionsTheme.colors.backgroundContainer)
                        .fillMaxWidth()
                        .clickable { onInstitutionSelected(institution) },
                ) {
                    Text(
                        text = institution.name,
                        color = FinancialConnectionsTheme.colors.textPrimary,
                        style = FinancialConnectionsTheme.typography.bodyEmphasized,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}

@Composable
@Preview(
    showBackground = true
)
private fun InstitutionPickerPreview() {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            featuredInstitutions = Success(
                InstitutionResponse(
                    listOf(
                        Institution(
                            id = "1",
                            name = "Very very long institution 1",
                            url = "institution 1 url",
                            featured = false,
                            featuredOrder = null
                        ),
                        Institution(
                            id = "2",
                            name = "Institution 2",
                            url = "Institution 2 url",
                            featured = false,
                            featuredOrder = null
                        ),
                        Institution(
                            id = "3",
                            name = "Institution 3",
                            url = "Institution 3 url",
                            featured = false,
                            featuredOrder = null
                        )
                    )
                )
            ),
            searchInstitutions = Success(
                InstitutionResponse(
                    listOf(
                        Institution(
                            id = "1",
                            name = "Very very long institution 1",
                            url = "institution 1 url",
                            featured = false,
                            featuredOrder = null
                        ),
                        Institution(
                            id = "2",
                            name = "Institution 2",
                            url = "Institution 2 url",
                            featured = false,
                            featuredOrder = null
                        ),
                        Institution(
                            id = "3",
                            name = "Institution 3",
                            url = "Institution 3 url",
                            featured = false,
                            featuredOrder = null
                        )
                    )
                )
            ),
            searchMode = true,
            query = "",
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onSearchFocused = {},
        )
    }
}
