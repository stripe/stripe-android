package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.common.FullScreenGenericLoading
import com.stripe.android.financialconnections.features.common.InstitutionIcon
import com.stripe.android.financialconnections.features.common.LoadingShimmerEffect
import com.stripe.android.financialconnections.features.common.LoadingSpinner
import com.stripe.android.financialconnections.features.common.ShapedIcon
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.paneViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsColors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.typography
import com.stripe.android.financialconnections.ui.theme.LazyLayout
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch

@Composable
internal fun InstitutionPickerScreen(
    backStackEntry: NavBackStackEntry,
) {
    val viewModel: InstitutionPickerViewModel = paneViewModel {
        InstitutionPickerViewModel.factory(parentComponent = it, backStackEntry.arguments)
    }

    val state: InstitutionPickerState by viewModel.stateFlow.collectAsState()
    val listState = rememberLazyListState()

    InstitutionPickerContent(
        listState = listState,
        payload = state.payload,
        institutions = state.searchInstitutions,
        // This is just used to provide a text in Compose previews
        previewText = state.previewText,
        selectedInstitutionId = state.selectedInstitutionId,
        onQueryChanged = viewModel::onQueryChanged,
        onInstitutionSelected = viewModel::onInstitutionSelected,
        onManualEntryClick = viewModel::onManualEntryClick,
        onScrollChanged = viewModel::onScrollChanged,
    )
}

@Composable
private fun InstitutionPickerContent(
    listState: LazyListState,
    payload: Async<Payload>,
    institutions: Async<InstitutionResponse>,
    previewText: String?,
    selectedInstitutionId: String?,
    onQueryChanged: (String) -> Unit,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit,
    onManualEntryClick: () -> Unit,
    onScrollChanged: () -> Unit
) {
    Box {
        when (payload) {
            is Uninitialized,
            is Loading,
            is Fail -> FullScreenGenericLoading()

            is Success -> LoadedContent(
                listState = listState,
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
    listState: LazyListState,
    previewText: String?,
    selectedInstitutionId: String?,
    onQueryChanged: (String) -> Unit,
    institutions: Async<InstitutionResponse>,
    onInstitutionSelected: (FinancialConnectionsInstitution, Boolean) -> Unit,
    payload: Payload,
    onManualEntryClick: () -> Unit,
    onScrollChanged: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(previewText ?: "") }
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

    LazyLayout(
        lazyListState = listState,
        bodyPadding = PaddingValues(horizontal = 16.dp),
    ) {
        item { SearchTitle(modifier = Modifier.padding(horizontal = 8.dp)) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        stickyHeader(key = "searchRow") {
            SearchRow(
                focusRequester = searchInputFocusRequester,
                query = input,
                onQueryChanged = {
                    input = it
                    onQueryChanged(input)
                },
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }

        searchResults(
            isInputEmpty = input.isBlank(),
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
}

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
            items(
                items = payload.featuredInstitutions.data,
                key = { it.id },
                itemContent = { institution ->
                    InstitutionResultTile(
                        modifier = Modifier.padding(8.dp),
                        loading = selectedInstitutionId == institution.id,
                        enabled = selectedInstitutionId?.let { it == institution.id } ?: true,
                        institution = institution,
                        onInstitutionSelected = { onInstitutionSelected(it, true) }
                    )
                }
            )
            item(key = "search_more") {
                SearchMoreRow(
                    modifier = Modifier.padding(8.dp),
                    onClick = onSearchMoreClick,
                    enabled = selectedInstitutionId == null,
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
                items(
                    items = institutions().data,
                    key = { it.id },
                    itemContent = { institution ->
                        InstitutionResultTile(
                            modifier = Modifier.padding(8.dp),
                            loading = selectedInstitutionId == institution.id,
                            enabled = selectedInstitutionId?.let { it == institution.id } ?: true,
                            institution = institution,
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
            style = typography.headingLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnnotatedText(
            text = when (showManualEntry ?: false) {
                true -> TextResource.StringId(R.string.stripe_institutionpicker_pane_error_desc_manual_entry)
                false -> TextResource.StringId(R.string.stripe_institutionpicker_pane_error_desc)
            },
            onClickableTextClick = { onManualEntryClick() },
            defaultStyle = typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = colors.textDefault
            ),
            annotationStyles = mapOf(
                StringAnnotation.CLICKABLE to typography.bodyMediumEmphasized.toSpanStyle().copy(
                    color = colors.textAction,
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
        style = typography.headingXLarge,
        color = colors.textDefault,
    )
}

@Composable
private fun SearchRow(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(top = 0.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
    ) {
        FinancialConnectionsOutlinedTextField(
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.stripe_ic_search),
                    tint = colors.icon,
                    contentDescription = "Search icon",
                )
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() },
            ),
            trailingIcon = query
                .takeIf { it.isNotEmpty() }
                ?.let {
                    {
                        ClearSearchButton(onQueryChanged = onQueryChanged, colors = colors)
                    }
                },
            placeholder = {
                Text(
                    text = stringResource(id = R.string.stripe_search),
                    style = typography.labelLarge,
                    color = colors.textSubdued
                )
            },
            value = query,
            enabled = true,
            onValueChange = { onQueryChanged(it) }
        )
    }
}

@Composable
private fun ClearSearchButton(
    onQueryChanged: (String) -> Unit,
    colors: FinancialConnectionsColors
) {
    Box(
        Modifier
            .size(16.dp)
            .clickable { onQueryChanged("") }
            .background(
                color = colors.textSubdued,
                shape = CircleShape
            )
            .padding(2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Clear,
            tint = colors.background,
            contentDescription = "Clear search",
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
        ShapedIcon(
            backgroundShape = RoundedCornerShape(12.dp),
            painter = painterResource(id = R.drawable.stripe_ic_add),
            contentDescription = "Manually enter details"
        )

        Spacer(modifier = Modifier.size(8.dp))
        Column {
            Text(
                text = stringResource(R.string.stripe_institutionpicker_manual_entry_title),
                color = colors.textDefault,
                style = typography.labelLargeEmphasized,
            )
            Text(
                text = stringResource(R.string.stripe_institutionpicker_manual_entry_desc),
                color = colors.textSubdued,
                style = typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchMoreRow(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxSize()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else DISABLED_DEPTH_ALPHA)
    ) {
        ShapedIcon(
            backgroundShape = RoundedCornerShape(12.dp),
            painter = painterResource(id = R.drawable.stripe_ic_search),
            contentDescription = "Add icon"
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.stripe_institutionpicker_search_more_title),
            color = colors.textDefault,
            style = typography.labelLargeEmphasized,
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
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag(institution.id)
            .clickable(
                enabled = enabled && loading.not(),
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                focusManager.clearFocus()
                onInstitutionSelected(institution)
            }
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
                color = colors.textDefault,
                style = typography.labelLargeEmphasized,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = institution.formattedUrl,
                color = colors.textSubdued,
                style = typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // add a trailing icon if this is the manual entry row
        if (loading) {
            Spacer(modifier = Modifier.size(8.dp))
            LoadingSpinner(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
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
    previewState: InstitutionPickerPreviewParameterProvider.InstitutionPreviewState
) {
    val state = previewState.state
    val listState = rememberLazyListState(
        initialFirstVisibleItemScrollOffset = previewState.initialScroll
    )
    FinancialConnectionsPreview {
        InstitutionPickerContent(
            listState = listState,
            payload = state.payload,
            institutions = state.searchInstitutions,
            previewText = state.previewText,
            selectedInstitutionId = state.selectedInstitutionId,
            onQueryChanged = {},
            onInstitutionSelected = { _, _ -> },
            onManualEntryClick = {},
            onScrollChanged = {},
        )
    }
}
