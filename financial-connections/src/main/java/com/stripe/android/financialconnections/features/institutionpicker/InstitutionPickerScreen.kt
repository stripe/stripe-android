@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
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
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.InstitutionIcon
import com.stripe.android.financialconnections.features.common.LoadingShimmerEffect
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.common.V3LoadingSpinner
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.Brand100
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import kotlinx.coroutines.launch

@Composable
internal fun InstitutionPickerScreen() {
    val viewModel: InstitutionPickerViewModel = mavericksViewModel()
    val parentViewModel = parentViewModel()
    val state: InstitutionPickerState by viewModel.collectAsState()

    InstitutionPickerContent(
        payload = state.payload,
        institutions = state.searchInstitutions,
        // This is just used to provide a text in Compose previews
        previewText = state.previewText,
        selectedInstitutionId = state.selectedInstitutionId,
        onQueryChanged = viewModel::onQueryChanged,
        onInstitutionSelected = viewModel::onInstitutionSelected,
        onCloseClick = { parentViewModel.onCloseWithConfirmationClick(Pane.INSTITUTION_PICKER) },
        onManualEntryClick = viewModel::onManualEntryClick,
        onScrollChanged = viewModel::onScrollChanged,
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun InstitutionPickerContent(
    payload: Async<Payload>,
    institutions: Async<InstitutionResponse>,
    previewText: String?,
    selectedInstitutionId: String?,
    onQueryChanged: (String) -> Unit,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onManualEntryClick: () -> Unit,
    onScrollChanged: () -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (payload) {
            is Uninitialized,
            is Loading -> FullScreenGenericLoading()

            is Fail -> UnclassifiedErrorContent(
                error = payload.error,
                onCloseFromErrorClick = onCloseFromErrorClick
            )

            is Success -> LoadedContent(
                previewText = previewText,
                selectedInstitutionId = selectedInstitutionId,
                onQueryChanged = onQueryChanged,
                institutions = institutions,
                onInstitutionSelected = onInstitutionSelected,
                payload = payload(),
                onManualEntryClick = onManualEntryClick,
                onScrollChanged = onScrollChanged
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoadedContent(
    previewText: String?,
    selectedInstitutionId: String?,
    onQueryChanged: (String) -> Unit,
    institutions: Async<InstitutionResponse>,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit,
    payload: Payload,
    onManualEntryClick: () -> Unit,
    onScrollChanged: () -> Unit,
) {
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf(TextFieldValue(previewText ?: "")) }
    var shouldEmitScrollEvent by remember { mutableStateOf(true) }
    val searchInputFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // Scroll event should be emitted just once per search
    LaunchedEffect(institutions) { shouldEmitScrollEvent = true }
    // Trigger onScrollChanged with the list of institutions when scrolling stops (true -> false)
    LaunchedEffect(listState.isScrollInProgress) {
        if (institutions()?.data?.isNotEmpty() == true &&
            !listState.isScrollInProgress &&
            shouldEmitScrollEvent
        ) {
            onScrollChanged()
            shouldEmitScrollEvent = false
        }
    }

    CompositionLocalProvider(
        // Disable overscroll as it does not play well with sticky headers.
        LocalOverscrollConfiguration provides null
    ) {
        LazyColumn(
            Modifier.padding(horizontal = 16.dp),
            state = listState,
            content = {
                item { SearchTitle(modifier = Modifier.padding(horizontal = 8.dp)) }
                item { Spacer(modifier = Modifier.height(24.dp)) }
                if (payload.searchDisabled.not()) {
                    stickyHeader(key = "searchRow") {
                        SearchRow(
                            focusRequester = searchInputFocusRequester,
                            query = input,
                            onQueryChanged = {
                                input = it
                                onQueryChanged(input.text)
                            },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                searchResults(
                    isInputEmpty = input.text.isBlank(),
                    payload = payload,
                    selectedInstitutionId = selectedInstitutionId,
                    onInstitutionSelected = onInstitutionSelected,
                    institutions = institutions,
                    onManualEntryClick = onManualEntryClick,
                    onSearchMoreClick = {
                        // Scroll to the top of the list and focus on the search input
                        coroutineScope.launch { listState.animateScrollToItem(index = 1) }
                        searchInputFocusRequester.requestFocus()
                    }
                )
            }
        )
    }
}

@Suppress("MagicNumber", "NestedBlockDepth")
private fun LazyListScope.searchResults(
    isInputEmpty: Boolean,
    payload: Payload,
    selectedInstitutionId: String?,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit,
    institutions: Async<InstitutionResponse>,
    onManualEntryClick: () -> Unit,
    onSearchMoreClick: () -> Unit
) {
    when {
        // No input: Display featured institutions.
        isInputEmpty -> {
            itemsIndexed(
                items = payload.featuredInstitutions.data,
                key = { _, institution -> institution.id },
                itemContent = { index, institution ->
                    InstitutionResultTile(
                        modifier = Modifier.padding(8.dp),
                        loading = selectedInstitutionId == institution.id,
                        enabled = selectedInstitutionId?.let { it == institution.id } ?: true,
                        institution = institution,
                        index = index,
                        onInstitutionSelected = { onInstitutionSelected(it, true) }
                    )
                }
            )
            item(key = "search_more") {
                SearchMoreRow(
                    modifier = Modifier.padding(8.dp),
                    onClick = onSearchMoreClick
                )
            }
        }

        else -> when (institutions) {
            // Load failure: Display error message.
            is Fail -> item {
                NoResultsTile(
                    modifier = Modifier.padding(8.dp),
                    showManualEntry = payload.featuredInstitutions.showManualEntry,
                    onManualEntryClick = onManualEntryClick
                )
            }

            // Loading: Display shimmer.
            is Uninitialized,
            is Loading -> items((0..10).toList()) {
                InstitutionResultShimmer(
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Success: Display search results.
            is Success -> if (institutions().data.isEmpty()) {
                // NO RESULTS CASE
                item {
                    NoResultsTile(
                        modifier = Modifier.padding(8.dp),
                        showManualEntry = institutions().showManualEntry,
                        onManualEntryClick = onManualEntryClick
                    )
                }
            } else {
                // RESULTS CASE: Institution List + Manual Entry final row if needed.
                itemsIndexed(
                    items = institutions().data,
                    key = { _, institution -> institution.id },
                    itemContent = { index, institution ->
                        InstitutionResultTile(
                            modifier = Modifier.padding(8.dp),
                            loading = selectedInstitutionId == institution.id,
                            enabled = selectedInstitutionId?.let { it == institution.id } ?: true,
                            institution = institution,
                            index = index,
                            onInstitutionSelected = { onInstitutionSelected(it, false) }
                        )
                    }
                )
                if (institutions().showManualEntry == true) {
                    item {
                        ManualEntryRow(
                            modifier = Modifier.padding(8.dp),
                            enabled = selectedInstitutionId == null,
                            onManualEntryClick = onManualEntryClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoResultsTile(
    modifier: Modifier = Modifier,
    showManualEntry: Boolean?,
    onManualEntryClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.stripe_institutionpicker_pane_error_title),
            style = v3Typography.headingLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnnotatedText(
            text = when (showManualEntry ?: false) {
                true -> TextResource.StringId(R.string.stripe_institutionpicker_pane_error_desc_manual_entry)
                false -> TextResource.StringId(R.string.stripe_institutionpicker_pane_error_desc)
            },
            onClickableTextClick = { onManualEntryClick() },
            defaultStyle = v3Typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to v3Typography.bodyMediumEmphasized.toSpanStyle().copy(
                    color = v3Colors.textBrand,
                )
            ),
        )
    }
}

@Composable
private fun SearchTitle(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = stringResource(R.string.stripe_institutionpicker_pane_select_bank),
        style = v3Typography.headingXLarge
    )
}

@Composable
private fun SearchRow(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    query: TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
) {
    Box {
        // Adds a top background to prevent search results from showing through the search bar
        Box(
            modifier = modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(12.dp)
                .background(v3Colors.backgroundSurface)
        )
        FinancialConnectionsOutlinedTextField(
            modifier = modifier
                .padding(horizontal = 6.dp)
                .fillMaxWidth()
                .focusRequester(focusRequester),
            leadingIcon = {
                Icon(
                    Icons.Filled.Search,
                    tint = v3Colors.iconDefault,
                    contentDescription = "Search icon",
                )
            },
            trailingIcon = if (query.text.isNotEmpty()) {
                {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clickable { onQueryChanged(TextFieldValue("")) }
                            .background(
                                color = v3Colors.border,
                                shape = CircleShape
                            )
                            .padding(2.dp)
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            tint = v3Colors.backgroundSurface,
                            contentDescription = "Clear search",
                        )
                    }
                }
            } else null,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.stripe_search),
                    style = v3Typography.labelLarge,
                    color = v3Colors.textSubdued
                )
            },
            value = query,
            onValueChange = { onQueryChanged(it) }
        )
    }
}

@Composable
private fun ManualEntryRow(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onManualEntryClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxSize()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onManualEntryClick
            )
            .alpha(if (enabled) 1f else DISABLED_DEPTH_ALPHA)
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            tint = v3Colors.iconBrand,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brand100)
                .padding(8.dp),
            contentDescription = "Add icon"
        )
        Spacer(modifier = Modifier.size(8.dp))
        Column {
            Text(
                text = stringResource(R.string.stripe_institutionpicker_manual_entry_title),
                color = v3Colors.textDefault,
                style = v3Typography.labelLargeEmphasized,
            )
            Text(
                text = stringResource(R.string.stripe_institutionpicker_manual_entry_desc),
                color = v3Colors.textSubdued,
                style = v3Typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchMoreRow(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            tint = v3Colors.iconBrand,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brand100)
                .padding(8.dp),
            contentDescription = "Add icon"
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.stripe_institutionpicker_search_more_title),
            color = v3Colors.textDefault,
            style = v3Typography.labelLargeEmphasized,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InstitutionResultTile(
    modifier: Modifier = Modifier,
    institution: FinancialConnectionsInstitution,
    loading: Boolean = false,
    enabled: Boolean = true,
    index: Int,
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag("search_result_$index")
            .clickable(
                enabled = enabled && loading.not(),
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onInstitutionSelected(institution) }
            .alpha(if (enabled) 1f else DISABLED_DEPTH_ALPHA)
    ) {
        InstitutionIcon(institution.icon?.default)
        Spacer(modifier = Modifier.size(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = institution.name,
                maxLines = 1,
                color = v3Colors.textDefault,
                style = v3Typography.labelLargeEmphasized,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = institution.url ?: "",
                color = v3Colors.textSubdued,
                style = v3Typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // add a trailing icon if this is the manual entry row
        if (loading) {
            Spacer(modifier = Modifier.size(8.dp))
            V3LoadingSpinner(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
@Suppress("MagicNumber")
private fun InstitutionResultShimmer(modifier: Modifier) {
    LoadingShimmerEffect { shimmer ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmer)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)

                )
                Spacer(modifier = Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
            }
        }
    }
}

private const val DISABLED_DEPTH_ALPHA = 0.3f

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
            previewText = state.previewText,
            selectedInstitutionId = state.selectedInstitutionId,
            onQueryChanged = {},
            onInstitutionSelected = { _, _ -> },
            onCloseClick = {},
            onManualEntryClick = {},
            onCloseFromErrorClick = {},
        ) {}
    }
}
