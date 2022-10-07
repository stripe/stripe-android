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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
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
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun InstitutionPickerScreen() {
    val viewModel: InstitutionPickerViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state by viewModel.collectAsState()

    // when in search mode, back closes search.
    BackHandler(state.searchMode, viewModel::onCancelSearchClick)

    InstitutionPickerContent(
        payload = state.payload,
        institutionsProvider = { state.searchInstitutions },
        searchMode = state.searchMode,
        query = state.query,
        onQueryChanged = viewModel::onQueryChanged,
        onInstitutionSelected = viewModel::onInstitutionSelected,
        onCancelSearchClick = viewModel::onCancelSearchClick,
        onCloseClick = parentViewModel::onCloseNoConfirmationClick,
        onSearchFocused = viewModel::onSearchFocused,
        onManualEntryClick = viewModel::onManualEntryClick
    )
}

@Composable
private fun InstitutionPickerContent(
    payload: Async<Payload>,
    institutionsProvider: () -> Async<InstitutionResponse>,
    searchMode: Boolean,
    query: String,
    onQueryChanged: (String) -> Unit,
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit,
    onCancelSearchClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSearchFocused: () -> Unit,
    onManualEntryClick: () -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            if (!searchMode) {
                FinancialConnectionsTopAppBar(
                    onCloseClick = onCloseClick
                )
            }
        }
    ) {
        LoadedContent(
            searchMode = searchMode,
            query = query,
            onQueryChanged = onQueryChanged,
            onSearchFocused = onSearchFocused,
            onCancelSearchClick = onCancelSearchClick,
            institutionsProvider = institutionsProvider,
            onInstitutionSelected = onInstitutionSelected,
            payload = payload,
            onManualEntryClick = onManualEntryClick
        )
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
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit,
    payload: Async<Payload>,
    onManualEntryClick: () -> Unit
) {
    Column(
        modifier = Modifier
    ) {
        if (searchMode.not()) {
            Spacer(Modifier.size(24.dp))
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.stripe_institutionpicker_pane_select_bank),
                style = FinancialConnectionsTheme.typography.subtitle
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        if (payload()?.searchDisabled == false) {
            FinancialConnectionsSearchRow(
                query = query,
                searchMode = searchMode,
                onQueryChanged = onQueryChanged,
                onSearchFocused = onSearchFocused,
                onCancelSearchClick = onCancelSearchClick
            )
        }
        if (query.isNotEmpty()) {
            SearchInstitutionsList(
                institutionsProvider = institutionsProvider,
                onInstitutionSelected = onInstitutionSelected,
                query = query,
                onManualEntryClick = onManualEntryClick,
                manualEntryEnabled = payload()?.allowManualEntry ?: false
            )
        } else {
            FeaturedInstitutionsGrid(
                payload = payload,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
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
            } else {
                null
            },
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
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit,
    query: String,
    onManualEntryClick: () -> Unit,
    manualEntryEnabled: Boolean
) {
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 16.dp),
        content = {
            when (val institutions: Async<InstitutionResponse> = institutionsProvider()) {
                Uninitialized,
                is Fail -> item {
                    SearchInstitutionsFailedRow(
                        onManualEntryClick = onManualEntryClick,
                        manualEntryEnabled = manualEntryEnabled
                    )
                }

                is Loading -> item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) { LoadingSpinner() }
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
                    item {
                        Column {
                            Spacer(modifier = Modifier.size(16.dp))
                            SearchFooter(
                                onManualEntryClick = onManualEntryClick,
                                manualEntryEnabled = manualEntryEnabled
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun SearchInstitutionsFailedRow(
    manualEntryEnabled: Boolean,
    onManualEntryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.stripe_ic_warning),
            contentDescription = "Warning icon",
            tint = FinancialConnectionsTheme.colors.textSecondary
        )
        Text(
            text = stringResource(id = R.string.stripe_institutionpicker_pane_error_title),
            style = FinancialConnectionsTheme.typography.body,
            color = FinancialConnectionsTheme.colors.textSecondary
        )
        when {
            manualEntryEnabled -> AnnotatedText(
                modifier = Modifier.fillMaxWidth(),
                text = TextResource.StringId(
                    R.string.stripe_institutionpicker_pane_error_desc_manual_entry
                ),
                onClickableTextClick = { onManualEntryClick() },
                defaultStyle = FinancialConnectionsTheme.typography.body.copy(
                    textAlign = TextAlign.Center,
                    color = FinancialConnectionsTheme.colors.textSecondary
                )
            )

            else -> Text(
                text = stringResource(id = R.string.stripe_institutionpicker_pane_error_desc),
                style = FinancialConnectionsTheme.typography.body,
                color = FinancialConnectionsTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun InstitutionResultTile(
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit,
    institution: FinancialConnectionsInstitution
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .clickable { onInstitutionSelected(institution) }
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            )
    ) {
        StripeImage(
            url = institution.icon?.default ?: "",
            errorContent = {
                Image(
                    painter = painterResource(id = R.drawable.stripe_ic_brandicon_institution),
                    contentDescription = "Bank icon placeholder"
                )
            },
            contentScale = ContentScale.Crop,
            contentDescription = null,
            imageLoader = LocalImageLoader.current,
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
    payload: Async<Payload>,
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            top = 16.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            when (payload) {
                Uninitialized, is Loading -> {
                    item(span = { GridItemSpan(2) }) {
                        LoadingSpinner()
                    }
                }
                // Show empty featured institutions grid. Users will be able to search using search bar.
                is Fail -> Unit
                is Success -> items(payload().featuredInstitutions) { institution ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = 1.dp,
                                color = FinancialConnectionsTheme.colors.borderDefault,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onInstitutionSelected(institution) }
                    ) {
                        StripeImage(
                            modifier = Modifier.fillMaxSize(),
                            url = institution.logo?.default ?: "",
                            imageLoader = LocalImageLoader.current,
                            contentScale = ContentScale.Crop,
                            errorContent = {
                                Text(
                                    text = institution.name,
                                    color = FinancialConnectionsTheme.colors.textPrimary,
                                    style = FinancialConnectionsTheme.typography.bodyEmphasized,
                                    textAlign = TextAlign.Center
                                )
                            },
                            contentDescription = "Institution logo"
                        )
                    }
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
            payload = state.payload,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onCloseClick = {},
            onSearchFocused = {}
        ) {}
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "searchModeWithResults")
internal fun SearchModeWithResults(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeWithResults()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            payload = state.payload,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onCloseClick = {},
            onSearchFocused = {}
        ) {}
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "searchModeNoResults")
internal fun SearchModeNoResults(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeNoResults()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            payload = state.payload,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onCloseClick = {},
            onSearchFocused = {}
        ) {}
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "searchModeFailed")
internal fun SearchModeFailed(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeFailed()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            payload = state.payload,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onCloseClick = {},
            onSearchFocused = {}
        ) {}
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "searchModeNoQuery")
internal fun SearchModeNoQuery(
    state: InstitutionPickerState = InstitutionPickerStates.searchModeNoQuery()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            payload = state.payload,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onCloseClick = {},
            onSearchFocused = {}
        ) {}
    }
}

@Composable
@Preview(group = "Institutions Pane", name = "noSearchMode")
internal fun NoSearchMode(
    state: InstitutionPickerState = InstitutionPickerStates.noSearchMode()
) {
    FinancialConnectionsTheme {
        InstitutionPickerContent(
            payload = state.payload,
            institutionsProvider = { state.searchInstitutions },
            searchMode = state.searchMode,
            query = state.query,
            onQueryChanged = {},
            onInstitutionSelected = {},
            onCancelSearchClick = {},
            onCloseClick = {},
            onSearchFocused = {}
        ) {}
    }
}
