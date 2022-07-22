package com.stripe.android.paymentsheet.addresselement

import android.app.Application
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.ui.AddressOptionsAppBar
import com.stripe.android.ui.core.darken
import com.stripe.android.ui.core.elements.TextFieldSection
import com.stripe.android.ui.core.elements.annotatedStringResource
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.ui.core.injection.NonFallbackInjector
import com.stripe.android.ui.core.paymentsColors

@VisibleForTesting
const val TEST_TAG_ATTRIBUTION_DRAWABLE = "AutocompleteAttributionDrawable"

@Composable
internal fun AutocompleteScreen(
    injector: NonFallbackInjector,
    country: String?
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: AutocompleteViewModel =
        viewModel<AutocompleteViewModel>(
            factory = AutocompleteViewModel.Factory(
                injector,
                AutocompleteViewModel.Args(
                    country = country
                )
            ) { application }
        ).also {
            it.initialize()
        }

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
    Scaffold(
        bottomBar = {
            val background = if (isSystemInDarkTheme()) {
                MaterialTheme.paymentsColors.component
            } else {
                MaterialTheme.paymentsColors.materialColors.surface.darken(0.07f)
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .systemBarsPadding()
                .padding(paddingValues)
                .background(MaterialTheme.colors.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                AddressOptionsAppBar(false) {
                    viewModel.setResultAndGoBack()
                }
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
                    SideEffect {
                        focusRequester.requestFocus()
                    }
                }
                if (loading) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (query.value.isNotBlank()) {
                    predictions?.let {
                        if (it.isNotEmpty()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
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
                                            color = MaterialTheme.paymentsColors.onComponent,
                                            style = MaterialTheme.typography.body1
                                        )
                                        Text(
                                            text = secondaryText.toString(),
                                            color = MaterialTheme.paymentsColors.onComponent,
                                            style = MaterialTheme.typography.body1
                                        )
                                    }
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.stripe_paymentsheet_autocomplete_no_results_found
                                    ),
                                    color = MaterialTheme.paymentsColors.onComponent,
                                    style = MaterialTheme.typography.body1
                                )
                            }
                        }
                        attributionDrawable?.let { drawable ->
                            Image(
                                painter = painterResource(
                                    id = drawable
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .padding(horizontal = 16.dp)
                                    .testTag(TEST_TAG_ATTRIBUTION_DRAWABLE)
                            )
                        }
                    }
                }
            }
        }
    }
}
