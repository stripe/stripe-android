@file:Suppress("TooManyFunctions", "LongMethod")

package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.stripe.android.financialconnections.features.common.V3LoadingSpinner
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsOutlinedTextField
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.theme.Brand100
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Colors
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme.v3Typography
import com.stripe.android.financialconnections.ui.theme.Layout
import com.stripe.android.uicore.image.StripeImage

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
        onScrollChanged = viewModel::onScrollChanged
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
            Uninitialized -> {} //TODO
            is Fail -> {} //TODO
            is Loading -> {} //TODO
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
    var input by remember { mutableStateOf(TextFieldValue(previewText ?: "")) }
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

    Layout(
        lazyListState = listState,
        content = {
            item { SearchTitle() }
            item { Spacer(modifier = Modifier.height(24.dp)) }
            if (payload.searchDisabled.not()) {
                stickyHeader {
                    SearchRow(
                        query = input,
                        onQueryChanged = {
                            input = it
                            onQueryChanged(input.text)
                        },
                    )
                }
            }

            if (input.text.isBlank()) {
                itemsIndexed(
                    items = payload.featuredInstitutions.data,
                    key = { _, institution -> institution.id },
                    itemContent = { index, institution ->
                        InstitutionResultTile(
                            loading = selectedInstitutionId == institution.id,
                            enabled = selectedInstitutionId?.let { it == institution.id } ?: true,
                            institution = institution,
                            index = index,
                            onInstitutionSelected = { onInstitutionSelected(it, true) }
                        )
                    }
                )
            } else when (institutions) {
                is Fail -> item {
                    NoResultsTile(
                        showManualEntry = payload.featuredInstitutions.showManualEntry,
                        onManualEntryClick = onManualEntryClick
                    )
                }

                Uninitialized,
                is Loading -> items((0..10).toList()) {
                    InstitutionResultShimmer()
                }

                is Success -> if (institutions().data.isEmpty()) {
                    // NO RESULTS CASE
                    item {
                        NoResultsTile(
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
                                enabled = selectedInstitutionId != null,
                                onManualEntryClick = onManualEntryClick
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun NoResultsTile(
    showManualEntry: Boolean?,
    onManualEntryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(top = 32.dp)
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
private fun SearchTitle() {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.stripe_institutionpicker_pane_select_bank),
        style = v3Typography.headingXLarge
    )
}

@Composable
private fun SearchRow(
    query: TextFieldValue,
    onQueryChanged: (TextFieldValue) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colors.backgroundSurface),
    ) {
        FinancialConnectionsOutlinedTextField(
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            leadingIcon = if (query.text.isNotEmpty()) {
                {
                    Icon(
                        Icons.Filled.ArrowBack,
                        tint = colors.textPrimary,
                        contentDescription = "Back button",
                        modifier = Modifier.clickable {
                            onQueryChanged(TextFieldValue(""))
                            focusManager.clearFocus()
                        }
                    )
                }
            } else {
                {
                    Icon(
                        Icons.Filled.Search,
                        tint = colors.textPrimary,
                        contentDescription = "Search icon",
                    )
                }
            },
            placeholder = {
                Text(
                    text = stringResource(id = R.string.stripe_search),
                    style = FinancialConnectionsTheme.typography.body,
                    color = colors.textDisabled
                )
            },
            value = query,
            onValueChange = { onQueryChanged(it) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ManualEntryRow(enabled: Boolean, onManualEntryClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onManualEntryClick
            )
            .alpha(if (enabled) 1f else 0.3f)
            .padding(vertical = 8.dp)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun InstitutionResultTile(
    institution: FinancialConnectionsInstitution,
    loading: Boolean = false,
    enabled: Boolean = true,
    index: Int,
    onInstitutionSelected: (FinancialConnectionsInstitution) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,

        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag("search_result_$index")
            .clickable(
                enabled = enabled && loading.not(),
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onInstitutionSelected(institution) }
            .alpha(if (enabled) 1f else 0.3f)
            .padding(vertical = 8.dp)
    ) {
        val modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(6.dp))
        when {
            institution.icon?.default.isNullOrEmpty() -> InstitutionPlaceholder(modifier)
            else -> StripeImage(
                url = requireNotNull(institution.icon?.default),
                imageLoader = LocalImageLoader.current,
                contentDescription = null,
                modifier = modifier,
                contentScale = ContentScale.Crop,
                loadingContent = {
                    LoadingShimmerEffect { Box(modifier = modifier.background(it)) }
                },
                errorContent = { InstitutionPlaceholder(modifier) }
            )
        }
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
private fun InstitutionResultShimmer() {
    LoadingShimmerEffect { shimmer ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            val modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
            Box(modifier = modifier.background(shimmer))
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
        ) {}
    }
}
