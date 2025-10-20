package com.stripe.android.paymentsheet.addresselement

import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.elements.autocomplete.model.AutocompletePrediction
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.TextField
import com.stripe.android.uicore.elements.TextFieldController
import com.stripe.android.uicore.elements.TextFieldSection
import com.stripe.android.uicore.getOuterFormInsets
import com.stripe.android.uicore.shouldUseDarkDynamicColor
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.annotatedStringResource
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Provider

@VisibleForTesting
internal const val TEST_TAG_ATTRIBUTION_DRAWABLE = "AutocompleteAttributionDrawable"

@Composable
internal fun AutocompleteScreen(
    autoCompleteViewModelSubcomponentBuilderProvider: Provider<AutocompleteViewModelSubcomponent.Builder>,
    navigator: AddressElementNavigator,
    country: String?
) {
    val viewModel: AutocompleteViewModel =
        viewModel(
            factory = AutocompleteViewModel.Factory(
                autoCompleteViewModelSubcomponentBuilderProvider = autoCompleteViewModelSubcomponentBuilderProvider,
                args = AutocompleteViewModel.Args(
                    country = country
                ),
            )
        )

    AutocompleteScreenUI(viewModel = viewModel, navigator = navigator)
}

@Composable
internal fun AutocompleteScreenUI(
    viewModel: AutocompleteViewModel,
    navigator: AddressElementNavigator,
    attributionDrawable: Int? =
        PlacesClientProxy.getPlacesPoweredByGoogleDrawable(isSystemInDarkTheme())
) {
    LaunchedEffect(Unit) {
        viewModel.event.collectLatest { event ->
            when (event) {
                is AutocompleteViewModel.Event.GoBack -> {
                    navigator.setResult(
                        AddressElementNavigator.AutocompleteEvent.KEY,
                        AddressElementNavigator.AutocompleteEvent.OnBack(event.address)
                    )
                }
                is AutocompleteViewModel.Event.EnterManually -> {
                    navigator.setResult(
                        AddressElementNavigator.AutocompleteEvent.KEY,
                        AddressElementNavigator.AutocompleteEvent.OnEnterManually(event.address)
                    )
                }
            }

            navigator.onBack()
        }
    }

    AutocompleteScreenUI(
        viewModel = viewModel,
        attributionDrawable = attributionDrawable,
        isRootScreen = false,
        backgroundColor = MaterialTheme.colors.surface,
        appBar = { isRoot, onBack ->
            AddressOptionsAppBar(isRootScreen = isRoot) {
                onBack()
            }
        },
    )
}

@Composable
internal fun AutocompleteScreenUI(
    viewModel: AutocompleteViewModel,
    appearanceContext: AutocompleteAppearanceContext,
    attributionDrawable: Int?,
    isRootScreen: Boolean,
) {
    val predictions by viewModel.predictions.collectAsState()
    val loading by viewModel.loading.collectAsState()

    AutocompleteScreenUI(
        predictions = predictions,
        loading = loading,
        queryController = viewModel.textFieldController,
        attributionDrawable = attributionDrawable,
        appearanceContext = appearanceContext,
        isRootScreen = isRootScreen,
        onBackPressed = viewModel::onBackPressed,
        onEnterManually = viewModel::onEnterAddressManually,
        onSelectPrediction = viewModel::selectPrediction,
    )
}

@Composable
internal fun AutocompleteScreenUI(
    viewModel: AutocompleteViewModel,
    attributionDrawable: Int?,
    isRootScreen: Boolean,
    backgroundColor: Color,
    appBar: @Composable (isRootScreen: Boolean, onBack: () -> Unit) -> Unit,
) {
    val predictions by viewModel.predictions.collectAsState()
    val loading by viewModel.loading.collectAsState()

    AutocompleteScreenUI(
        predictions = predictions,
        loading = loading,
        queryController = viewModel.textFieldController,
        attributionDrawable = attributionDrawable,
        isRootScreen = isRootScreen,
        backgroundColor = backgroundColor,
        appBar = appBar,
        onBackPressed = viewModel::onBackPressed,
        onEnterManually = viewModel::onEnterAddressManually,
        onSelectPrediction = viewModel::selectPrediction,
    )
}

@Composable
internal fun AutocompleteScreenUI(
    predictions: List<AutocompletePrediction>?,
    loading: Boolean,
    queryController: TextFieldController,
    attributionDrawable: Int?,
    appearanceContext: AutocompleteAppearanceContext,
    isRootScreen: Boolean,
    onBackPressed: () -> Unit,
    onEnterManually: () -> Unit,
    onSelectPrediction: (AutocompletePrediction) -> Unit,
) {
    AutocompleteScreenUI(
        predictions = predictions,
        loading = loading,
        queryController = queryController,
        attributionDrawable = attributionDrawable,
        isRootScreen = isRootScreen,
        onBackPressed = onBackPressed,
        onEnterManually = onEnterManually,
        onSelectPrediction = onSelectPrediction,
        backgroundColor = appearanceContext.backgroundColor,
        appBar = { isRoot, onBack ->
            appearanceContext.AppBar(isRoot, onBack)
        }
    )
}

@Composable
internal fun AutocompleteScreenUI(
    predictions: List<AutocompletePrediction>?,
    loading: Boolean,
    queryController: TextFieldController,
    attributionDrawable: Int?,
    backgroundColor: Color,
    isRootScreen: Boolean,
    appBar: @Composable (isRootScreen: Boolean, onBack: () -> Unit) -> Unit,
    onBackPressed: () -> Unit,
    onEnterManually: () -> Unit,
    onSelectPrediction: (AutocompletePrediction) -> Unit,
) {
    val query by queryController.fieldValue.collectAsState()

    Scaffold(
        topBar = {
            appBar(isRootScreen, onBackPressed)
        },
        bottomBar = {
            val background = if (MaterialTheme.stripeColors.materialColors.surface.shouldUseDarkDynamicColor()) {
                Color.Black.copy(alpha = 0.07f)
            } else {
                Color.White.copy(alpha = 0.07f)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(color = background)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                EnterManuallyText {
                    onEnterManually()
                }
            }
        },
        backgroundColor = backgroundColor,
    ) { paddingValues ->
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                focusRequester.requestFocus()
            }
        }

        ScrollableColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .systemBarsPadding()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = StripeTheme.formInsets.top.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    TextFieldSection(
                        textFieldController = queryController,
                        modifier = Modifier.padding(StripeTheme.getOuterFormInsets())
                    ) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textFieldController = queryController,
                            imeAction = ImeAction.Done,
                            enabled = true,
                        )
                    }
                }
                if (loading) {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (query.isNotBlank()) {
                    predictions?.let {
                        if (it.isNotEmpty()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(StripeTheme.getOuterFormInsets())
                            ) {
                                it.forEach { prediction ->
                                    val primaryText = prediction.primaryText
                                    val secondaryText = prediction.secondaryText
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onSelectPrediction(prediction)
                                            }
                                            .padding(
                                                vertical = 8.dp
                                            )
                                    ) {
                                        val regex = query
                                            .replace(" ", "|")
                                            .toRegex(RegexOption.IGNORE_CASE)
                                        val matches = regex.findAll(primaryText).toList()
                                        val values = matches.map {
                                            it.value
                                        }.filter { it.isNotBlank() }
                                        var text = primaryText.toString()
                                        values.forEach {
                                            text = text.replace(it, "<b>$it</b>")
                                        }
                                        Text(
                                            text = annotatedStringResource(text = text),
                                            color = MaterialTheme.stripeColors.onComponent,
                                            style = MaterialTheme.typography.body1
                                        )
                                        Text(
                                            text = secondaryText.toString(),
                                            color = MaterialTheme.stripeColors.onComponent,
                                            style = MaterialTheme.typography.body1
                                        )
                                    }
                                    Divider()
                                }
                            }
                        }

                        attributionDrawable?.let { drawable ->
                            Image(
                                painter = painterResource(
                                    id = drawable
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .padding(StripeTheme.getOuterFormInsets())
                                    .testTag(TEST_TAG_ATTRIBUTION_DRAWABLE)
                            )
                        }
                    }
                }
            }
        }
    }
}
