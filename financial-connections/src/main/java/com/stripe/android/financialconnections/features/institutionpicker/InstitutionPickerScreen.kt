@file:Suppress("TooManyFunctions")

package com.stripe.android.financialconnections.features.institutionpicker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.exception.InstitutionPlannedException
import com.stripe.android.financialconnections.exception.InstitutionUnplannedException
import com.stripe.android.financialconnections.features.common.InstitutionPlannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnplannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme

@Composable
internal fun InstitutionPickerScreen() {
    val viewModel: InstitutionPickerViewModel = mavericksViewModel()
    val state by viewModel.collectAsState()

    // when in select institution error, back goes back to bank selection.
    BackHandler(state.selectInstitution is Fail, viewModel::onSelectAnotherBank)
    // when in search mode, back closes search.
    BackHandler(state.searchMode, viewModel::onCancelSearchClick)

    InstitutionPickerContent(
        featuredInstitutions = state.featuredInstitutions,
        institutionsProvider = { state.searchInstitutions },
        selectInstitution = state.selectInstitution,
        searchMode = state.searchMode,
        query = state.query,
        onQueryChanged = { viewModel.onQueryChanged(it) },
        onInstitutionSelected = { viewModel.onInstitutionSelected(it) },
        onCancelSearchClick = { viewModel.onCancelSearchClick() },
        onSearchFocused = { viewModel.onSearchFocused() },
        onSelectAnotherBank = { viewModel.onSelectAnotherBank() },
    )
}

@Composable
private fun InstitutionPickerContent(
    featuredInstitutions: Async<InstitutionResponse>,
    institutionsProvider: () -> Async<InstitutionResponse>,
    selectInstitution: Async<Unit>,
    searchMode: Boolean,
    query: String,
    onQueryChanged: (String) -> Unit,
    onInstitutionSelected: (Institution) -> Unit,
    onSelectAnotherBank: () -> Unit,
    onCancelSearchClick: () -> Unit,
    onSearchFocused: () -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            if (!searchMode) {
                FinancialConnectionsTopAppBar()
            }
        }
    ) {
        when (selectInstitution) {
            is Uninitialized,
            is Success -> {
                LoadedContent(
                    searchMode = searchMode,
                    query = query,
                    onQueryChanged = onQueryChanged,
                    onSearchFocused = onSearchFocused,
                    onCancelSearchClick = onCancelSearchClick,
                    institutionsProvider = institutionsProvider,
                    onInstitutionSelected = onInstitutionSelected,
                    featuredInstitutions = featuredInstitutions
                )
            }
            is Loading -> {
                LoadingContent(
                    R.string.stripe_picker_loading_title,
                    R.string.stripe_picker_loading_desc
                )
            }
            is Fail -> {
                InstitutionPickerErrorContent(
                    error = selectInstitution.error,
                    onSelectAnotherBank = onSelectAnotherBank
                )
            }
        }
    }
}

@Composable
private fun InstitutionPickerErrorContent(
    error: Throwable,
    onSelectAnotherBank: () -> Unit
) {
    when (error) {
        is InstitutionPlannedException -> InstitutionPlannedDowntimeErrorContent(
            error,
            onSelectAnotherBank
        )
        is InstitutionUnplannedException -> InstitutionUnplannedDowntimeErrorContent(
            error,
            onSelectAnotherBank
        )
        else -> UnclassifiedErrorContent()
    }
}

@Composable
private fun LoadedContent(
    searchMode: Boolean,
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchFocused: () -> Unit,
    onCancelSearchClick: () -> Unit,
    institutionsProvider: () -> Async<InstitutionResponse>,
    onInstitutionSelected: (Institution) -> Unit,
    featuredInstitutions: Async<InstitutionResponse>
) {
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
        if (query.isNotEmpty()) {
            SearchInstitutionsList(
                query = query,
                institutionsProvider = institutionsProvider,
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
            leadingIcon = if (searchMode) {
                {
                    Icon(
                        Icons.Filled.ArrowBack,
                        tint = FinancialConnectionsTheme.colors.textPrimary,
                        contentDescription = "Back button",
                        modifier = Modifier.clickable {
                            onCancelSearchClick()
                            focusManager.clearFocus()
                        }
                    )
                }
            } else null,
            modifier = Modifier
                .onFocusChanged { if (it.isFocused) onSearchFocused() }
                .weight(1f),
            value = query,
            label = { Text(text = stringResource(id = R.string.stripe_search)) },
            onValueChange = onQueryChanged
        )
    }
}

@Composable
private fun SearchInstitutionsList(
    institutionsProvider: () -> Async<InstitutionResponse>,
    onInstitutionSelected: (Institution) -> Unit,
    query: String,
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 16.dp),
        content = {
            when (val institutions: Async<InstitutionResponse> = institutionsProvider()) {
                Uninitialized,
                is Fail -> Unit
                is Loading -> item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = FinancialConnectionsTheme.colors.textBrand
                        )
                    }
                }
                is Success -> {

                    if (institutions().data.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(
                                    R.string.stripe_picker_search_no_results,
                                    query
                                ),
                                style = FinancialConnectionsTheme.typography.caption,
                                color = FinancialConnectionsTheme.colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(institutions().data, key = { it.id }) { institution ->
                            InstitutionResultTile(onInstitutionSelected, institution)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun InstitutionResultTile(
    onInstitutionSelected: (Institution) -> Unit,
    institution: Institution
) {
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
                style = FinancialConnectionsTheme.typography.bodyEmphasized
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
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = FinancialConnectionsTheme.colors.borderDefault,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { onInstitutionSelected(institution) }
                        .padding(8.dp)
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
@Preview(group = "Institutions Pane", name = "searchModeSearchingInstitutions")
internal fun SearchModeSearchingInstitutions(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeSearchingInstitutions()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            selectInstitution = state.selectInstitution,
            featuredInstitutions = state.featuredInstitutions,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onSearchFocused = {},
            onSelectAnotherBank = {}
        )
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "searchModeWithResults")
internal fun SearchModeWithResults(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeWithResults()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            selectInstitution = state.selectInstitution,
            featuredInstitutions = state.featuredInstitutions,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onSearchFocused = {},
            onSelectAnotherBank = {}
        )
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "searchModeSelectingInstitutions")
internal fun SearchModeSelectingInstitutions(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeSelectingInstitutions()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            selectInstitution = state.selectInstitution,
            featuredInstitutions = state.featuredInstitutions,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onSearchFocused = {},
            onSelectAnotherBank = {}
        )
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "searchModeNoResults")
internal fun SearchModeNoResults(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeNoResults()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            selectInstitution = state.selectInstitution,
            featuredInstitutions = state.featuredInstitutions,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onSearchFocused = {},
            onSelectAnotherBank = {}
        )
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "searchModeNoQuery")
internal fun SearchModeNoQuery(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeNoQuery()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            selectInstitution = state.selectInstitution,
            featuredInstitutions = state.featuredInstitutions,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onSearchFocused = {},
            onSelectAnotherBank = {}
        )
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "noSearchMode")
internal fun NoSearchMode(
    state: InstitutionPickerState = InstitutionPickerStates.noSearchMode()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            selectInstitution = state.selectInstitution,
            featuredInstitutions = state.featuredInstitutions,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onSearchFocused = {},
            onSelectAnotherBank = {}
        )
    }
}
