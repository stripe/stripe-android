package com.stripe.android.paymentsheet.addresselement

import android.app.Application
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.common.ui.LoadingIndicator
import com.stripe.android.paymentsheet.injection.AutocompleteViewModelSubcomponent
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.uicore.darken
import com.stripe.android.uicore.elements.TextFieldSection
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.annotatedStringResource
import javax.inject.Provider

@VisibleForTesting
internal const val TEST_TAG_ATTRIBUTION_DRAWABLE = "AutocompleteAttributionDrawable"

@Composable
internal fun AutocompleteScreen(
    autoCompleteViewModelSubcomponentBuilderProvider: Provider<AutocompleteViewModelSubcomponent.Builder>,
    country: String?
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AutocompleteViewModel =
        viewModel(
            factory = AutocompleteViewModel.Factory(
                autoCompleteViewModelSubcomponentBuilderProvider = autoCompleteViewModelSubcomponentBuilderProvider,
                args = AutocompleteViewModel.Args(
                    country = country
                ),
                applicationSupplier = { application }
            )
        )

    AutocompleteScreenUI(viewModel = viewModel)
}

@Composable
internal fun AutocompleteScreenUI(viewModel: AutocompleteViewModel) {
    val predictions by viewModel.predictions.collectAsState()
    val loading by viewModel.loading.collectAsState(initial = false)
    val query = viewModel.textFieldController.fieldValue.collectAsState(initial = "")
    val attributionDrawable =
        PlacesClientProxy.getPlacesPoweredByGoogleDrawable(isSystemInDarkTheme())
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            AddressOptionsAppBar(false) {
                viewModel.onBackPressed()
            }
        },
        bottomBar = {
            val background = if (isSystemInDarkTheme()) {
                MaterialTheme.stripeColors.component
            } else {
                MaterialTheme.stripeColors.materialColors.surface.darken(0.07f)
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
                    viewModel.onEnterAddressManually()
                }
            }
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        ScrollableColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .systemBarsPadding()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TextFieldSection(
                        textFieldController = viewModel.textFieldController,
                        imeAction = ImeAction.Done,
                        enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                if (loading) {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (query.value.isNotBlank()) {
                    predictions?.let {
                        if (it.isNotEmpty()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                it.forEach { prediction ->
                                    val primaryText = prediction.primaryText
                                    val secondaryText = prediction.secondaryText
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.selectPrediction(prediction)
                                            }
                                            .padding(
                                                vertical = 8.dp,
                                                horizontal = 16.dp
                                            )
                                    ) {
                                        val regex = query.value
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
                                    Divider(
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
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
                                    .padding(
                                        vertical = 16.dp,
                                        horizontal = 16.dp
                                    )
                                    .testTag(TEST_TAG_ATTRIBUTION_DRAWABLE)
                            )
                        }
                    }
                }
            }
        }
    }
}
