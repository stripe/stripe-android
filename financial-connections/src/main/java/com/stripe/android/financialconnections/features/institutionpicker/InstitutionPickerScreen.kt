@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.institutionpicker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.InstitutionPlaceholder
import com.stripe.android.financialconnections.features.common.LoadingShimmerEffect
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.clickableSingle
import com.stripe.android.financialconnections.ui.theme.Brand100
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage

@Composable
internal fun InstitutionPickerScreen() {
    val viewModel: InstitutionPickerViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state: InstitutionPickerState by viewModel.collectAsState()

    // when in search mode, back closes search.
    val focusManager = LocalFocusManager.current
    BackHandler(state.searchMode) {
        focusManager.clearFocus()
        viewModel.onCancelSearchClick()
    }

    InstitutionPickerContent(
        payload = state.payload,
        institutions = state.searchInstitutions,
        searchMode = state.searchMode,
        // This is just used to provide a text in Compose previews
        previewText = state.previewText,
        onQueryChanged = viewModel::onQueryChanged,
        onInstitutionSelected = viewModel::onInstitutionSelected,
        onCancelSearchClick = viewModel::onCancelSearchClick,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.INSTITUTION_PICKER) },
        onSearchFocused = viewModel::onSearchFocused,
        onManualEntryClick = viewModel::onManualEntryClick,
        onScrollChanged = viewModel::onScrollChanged
    )
}

@Composable
private fun InstitutionPickerContent(
    payload: Async<Payload>,
    institutions: Async<InstitutionResponse>,
    searchMode: Boolean,
    previewText: String?,
    onQueryChanged: (String) -> Unit,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit,
    onCancelSearchClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSearchFocused: () -> Unit,
    onManualEntryClick: () -> Unit,
    onScrollChanged: () -> Unit
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
            previewText = previewText,
            onQueryChanged = onQueryChanged,
            onSearchFocused = onSearchFocused,
            onCancelSearchClick = onCancelSearchClick,
            institutions = institutions,
            onInstitutionSelected = onInstitutionSelected,
            payload = payload,
            onManualEntryClick = onManualEntryClick,
            onScrollChanged = onScrollChanged
        )
    }
}

@Composable
private fun LoadedContent(
    searchMode: Boolean,
    previewText: String?,
    onQueryChanged: (String) -> Unit,
    onSearchFocused: () -> Unit,
    onCancelSearchClick: () -> Unit,
    institutions: Async<InstitutionResponse>,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit,
    payload: Async<Payload>,
    onManualEntryClick: () -> Unit,
    onScrollChanged: () -> Unit,
) {
    var input by remember { mutableStateOf(TextFieldValue(previewText ?: "")) }
    LaunchedEffect(searchMode) { if (!searchMode) input = TextFieldValue() }
    Column {
        if (searchMode.not()) {
            Spacer(Modifier.size(16.dp))
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                text = stringResource(R.string.stripe_institutionpicker_pane_select_bank),
                style = FinancialConnectionsTheme.typography.subtitle
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        if (payload()?.searchDisabled == false) {
            FinancialConnectionsSearchRow(
                query = input,
                searchMode = searchMode,
                onQueryChanged = {
                    input = it
                    onQueryChanged(input.text)
                },
                onSearchFocused = onSearchFocused,
                onCancelSearchClick = onCancelSearchClick
            )
        }
        if (input.text.isNotBlank()) {
            SearchInstitutionsList(
                institutions = institutions,
                onInstitutionSelected = onInstitutionSelected,
                onManualEntryClick = onManualEntryClick,
                onScrollChanged = onScrollChanged,
                allowManualEntry = payload()?.allowManualEntry ?: false
            )
        } else {
            FeaturedInstitutionsGrid(
                modifier = Modifier.weight(1f),
                payload = payload,
                onInstitutionSelected = onInstitutionSelected
            )
        }
    }
}

@Composable
private fun FinancialConnectionsSearchRow(
    query: TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
    onCancelSearchClick: () -> Unit,
    onSearchFocused: () -> Unit,
    searchMode: Boolean
) {
    val focusManager = LocalFocusManager.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        FinancialConnectionsOutlinedTextField(
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
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
                {
                    Icon(
                        Icons.Filled.Search,
                        tint = FinancialConnectionsTheme.colors.textPrimary,
                        contentDescription = "Search icon",
                    )
                }
            },
            modifier = Modifier
                .onFocusChanged { if (it.isFocused) onSearchFocused() }
                .weight(1f),
            placeholder = {
                Text(
                    text = stringResource(id = R.string.stripe_search),
                    style = FinancialConnectionsTheme.typography.body,
                    color = FinancialConnectionsTheme.colors.textDisabled
                )
            },
            value = query,
            onValueChange = { onQueryChanged(it) }
        )
    }
}

@Composable
private fun SearchInstitutionsList(
    institutions: Async<InstitutionResponse>,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit,
    onManualEntryClick: () -> Unit,
    onScrollChanged: () -> Unit,
    allowManualEntry: Boolean
) {
    val listState = rememberLazyListState()
    val shouldEmitScrollEvent = remember { mutableStateOf(true) }

    // Scroll event should be emitted just once per search
    LaunchedEffect(institutions) { shouldEmitScrollEvent.value = true }
    // Trigger onScrollChanged with the list of institutions when scrolling stops (true -> false)
    LaunchedEffect(listState.isScrollInProgress) {
        if (institutions()?.data?.isNotEmpty() == true &&
            !listState.isScrollInProgress &&
            shouldEmitScrollEvent.value
        ) {
            onScrollChanged()
            shouldEmitScrollEvent.value = false
        }
    }

    LazyColumn(
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 16.dp),
        content = {
            when (institutions) {
                Uninitialized,
                is Fail -> item {
                    if (allowManualEntry) {
                        ManualEntryRow(onManualEntryClick)
                    } else {
                        NoResultsRow()
                    }
                }

                is Loading -> item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) { LoadingSpinner() }
                }

                is Success -> {
                    if (institutions().data.isEmpty()) {
                        // NO RESULTS CASE
                        item {
                            if (institutions().showManualEntry == true) {
                                ManualEntryRow(onManualEntryClick)
                            } else {
                                NoResultsRow()
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = institutions().data,
                            key = { _, institution -> institution.id },
                            itemContent = { index, institution ->
                                InstitutionResultTile(
                                    institution = institution,
                                    index = index
                                ) { onInstitutionSelected(it, false) }
                            }
                        )
                        if (institutions().showManualEntry == true) {
                            item {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 8.dp,
                                            horizontal = 24.dp
                                        ),
                                    color = FinancialConnectionsTheme.colors.borderDefault
                                )
                            }
                            item {
                                ManualEntryRow(onManualEntryClick)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun NoResultsRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = 8.dp,
                horizontal = 24.dp
            )
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.stripe_institutionpicker_no_results_title),
                color = FinancialConnectionsTheme.colors.textPrimary,
                style = FinancialConnectionsTheme.typography.bodyEmphasized
            )
            Text(
                text = stringResource(id = R.string.stripe_institutionpicker_no_results_desc),
                color = FinancialConnectionsTheme.colors.textSecondary,
                style = FinancialConnectionsTheme.typography.captionTight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ManualEntryRow(onManualEntryClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onManualEntryClick)
            .padding(
                vertical = 8.dp,
                horizontal = 24.dp
            )
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            tint = FinancialConnectionsTheme.colors.textBrand,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brand100)
                .padding(8.dp),
            contentDescription = "Add icon"
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            Text(
                text = stringResource(R.string.stripe_institutionpicker_manual_entry_title),
                color = FinancialConnectionsTheme.colors.textPrimary,
                style = FinancialConnectionsTheme.typography.bodyEmphasized
            )
            Text(
                text = stringResource(R.string.stripe_institutionpicker_manual_entry_desc),
                color = FinancialConnectionsTheme.colors.textSecondary,
                style = FinancialConnectionsTheme.typography.captionTight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InstitutionResultTile(
    institution: FinancialConnectionsInstitution,
    index: Int,
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag("search_result_$index")
            .clickableSingle { onInstitutionSelected(institution) }
            .padding(
                vertical = 8.dp,
                horizontal = 24.dp
            )
    ) {
        val modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
        when {
            institution.icon?.default.isNullOrEmpty() -> InstitutionPlaceholder(modifier)
            else -> StripeImage(
                url = requireNotNull(institution.icon?.default),
                imageLoader = LocalImageLoader.current,
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop,
                errorContent = { InstitutionPlaceholder(modifier) }
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            Text(
                text = institution.name,
                color = FinancialConnectionsTheme.colors.textPrimary,
                style = FinancialConnectionsTheme.typography.bodyEmphasized
            )
            Text(
                text = institution.url ?: "",
                color = FinancialConnectionsTheme.colors.textDisabled,
                style = FinancialConnectionsTheme.typography.captionTight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeaturedInstitutionsGrid(
    modifier: Modifier,
    payload: Async<Payload>,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            top = 16.dp,
            start = 24.dp,
            end = 24.dp,
            bottom = 16.dp
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
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickableSingle(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(
                                    color = FinancialConnectionsTheme.colors.textSecondary
                                ),
                            ) { onInstitutionSelected(institution, true) }
                    ) {
                        when {
                            institution.logo?.default.isNullOrBlank() ->
                                FeaturedInstitutionPlaceholder(institution)

                            else -> StripeImage(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                url = requireNotNull(institution.logo?.default),
                                imageLoader = LocalImageLoader.current,
                                contentScale = ContentScale.Fit,
                                loadingContent = { FeaturedInstitutionLoading() },
                                errorContent = { FeaturedInstitutionPlaceholder(institution) },
                                contentDescription = "Institution logo"
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun BoxScope.FeaturedInstitutionLoading() {
    LoadingShimmerEffect { shimmer ->
        Spacer(
            modifier = Modifier.Companion
                .align(Alignment.Center)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .fillMaxWidth(fraction = 0.5f)
                .background(shimmer)
        )
    }
}

@Composable
private fun BoxScope.FeaturedInstitutionPlaceholder(institution: FinancialConnectionsInstitution) {
    Text(
        modifier = Modifier.Companion.align(Alignment.Center),
        text = institution.name,
        color = FinancialConnectionsTheme.colors.textPrimary,
        style = FinancialConnectionsTheme.typography.bodyEmphasized,
        textAlign = TextAlign.Center
    )
}

@Preview(group = "Institution Picker Pane")
@Composable
internal fun InstitutionPickerPreview(
    @PreviewParameter(InstitutionPickerPreviewParameterProvider::class)
    state: InstitutionPickerState
) {
    FinancialConnectionsPreview {
        InstitutionPickerContent(
            payload = state.payload,
            institutions = state.searchInstitutions,
            searchMode = state.searchMode,
            previewText = state.previewText,
            onQueryChanged = {},
            onInstitutionSelected = { _, _ -> },
            onCancelSearchClick = {},
            onCloseClick = {},
            onSearchFocused = {},
            onManualEntryClick = {},
            onScrollChanged = {},
        )
    }
}
