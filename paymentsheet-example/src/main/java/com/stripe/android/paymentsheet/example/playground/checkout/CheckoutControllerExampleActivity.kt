@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentsheet.example.playground.PlaygroundTheme
import com.stripe.android.paymentsheet.example.playground.SearchSettingsField
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettingsUi
import com.stripe.android.paymentsheet.example.playground.checkout.settings.configurations
import kotlinx.coroutines.launch

internal class CheckoutControllerExampleActivity : AppCompatActivity() {
    private val viewModel: CheckoutControllerExampleViewModel by viewModels {
        CheckoutControllerExampleViewModel.factory
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = viewModel.controller.createPresenter(this)
        val paymentElement = presenter.paymentElement()
        val shippingAddressElement = presenter.shippingAddressElement()
        val currencySelectorElement = presenter.currencySelectorElement()
        val expressCheckoutElement = presenter.expressCheckoutElement()

        lifecycleScope.launch {
            viewModel.sessionComplete.collect {
                Toast.makeText(this@CheckoutControllerExampleActivity, "Payment complete!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setContent {
            val status by viewModel.status.collectAsState()
            val session by viewModel.controller.session.collectAsState()
            val confirmationMessage by viewModel.confirmationMessage.collectAsState()
            val operationMessage by viewModel.operationMessage.collectAsState()
            val isUpdating by viewModel.controller.isUpdating.collectAsState()
            val settingValues by viewModel.settings.values.collectAsState()
            var navigationPath by rememberSaveable {
                mutableStateOf<List<String>>(emptyList())
            }
            var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
            val isSearching = settingsSearchQuery.isNotBlank()
            val currentConfiguration = CheckoutPlaygroundDefinitions.root.configurations()
                .firstOrNull { it.key == navigationPath.lastOrNull() }
                ?: CheckoutPlaygroundDefinitions.root
            val navigateBack = {
                if (status is CheckoutControllerExampleViewModel.Status.Settings) {
                    if (isSearching) {
                        settingsSearchQuery = ""
                    } else {
                        navigationPath = navigationPath.dropLast(1)
                    }
                } else {
                    viewModel.returnToSettings()
                }
            }

            BackHandler(
                enabled = status !is CheckoutControllerExampleViewModel.Status.Settings ||
                    navigationPath.isNotEmpty() || isSearching
            ) { navigateBack() }

            PlaygroundTheme(
                topBarContent = {
                    Column {
                        TopAppBar(
                            windowInsets = AppBarDefaults.topAppBarWindowInsets,
                            title = {
                                Text(
                                    when {
                                        status !is CheckoutControllerExampleViewModel.Status.Settings -> "Checkout"
                                        isSearching -> "Search settings"
                                        else -> currentConfiguration.displayName
                                    }
                                )
                            },
                            navigationIcon = if (
                                status !is CheckoutControllerExampleViewModel.Status.Settings ||
                                navigationPath.isNotEmpty() || isSearching
                            ) {
                                {
                                    IconButton(onClick = navigateBack) {
                                        Text("‹", style = MaterialTheme.typography.h4)
                                    }
                                }
                            } else {
                                null
                            },
                        )
                        if (status is CheckoutControllerExampleViewModel.Status.Settings) {
                            SearchSettingsField(
                                query = settingsSearchQuery,
                                onQueryChanged = { settingsSearchQuery = it },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                },
                content = {
                    when (val currentStatus = status) {
                        CheckoutControllerExampleViewModel.Status.Settings -> {
                            CheckoutPlaygroundSettingsUi(
                                configuration = currentConfiguration,
                                searchQuery = settingsSearchQuery,
                                settings = viewModel.settings,
                                onOpenConfiguration = { navigationPath += it.key },
                                onOpenConfigurationPath = { configurationPath ->
                                    navigationPath = configurationPath.map { it.key }
                                    settingsSearchQuery = ""
                                },
                            )
                        }
                        CheckoutControllerExampleViewModel.Status.Loading -> LoadingContent()
                        is CheckoutControllerExampleViewModel.Status.Error -> ErrorContent(
                            message = currentStatus.message,
                            onRetry = viewModel::retry,
                            onBack = viewModel::returnToSettings,
                        )
                        CheckoutControllerExampleViewModel.Status.Configured -> {
                            session?.let { currentSession ->
                                CheckoutContent(
                                    session = currentSession,
                                    paymentElement = paymentElement,
                                    currencySelectorElement = currencySelectorElement,
                                    expressCheckoutElement = expressCheckoutElement,
                                    isUpdating = isUpdating,
                                    operationMessage = operationMessage,
                                    onApplyPromotionCode = viewModel::applyPromotionCode,
                                    onRemovePromotionCode = viewModel::removePromotionCode,
                                    onUpdateEmail = viewModel::updateEmail,
                                )
                            } ?: LoadingContent()
                        }
                    }
                },
                bottomBarContent = {
                    when (status) {
                        CheckoutControllerExampleViewModel.Status.Settings -> {
                            if (navigationPath.isEmpty()) {
                                SettingsActions(
                                    canStart = settingValues.isNotEmpty() &&
                                        viewModel.settings.validationErrors().isEmpty(),
                                    onStart = viewModel::start,
                                    onReset = viewModel.settings::reset,
                                )
                            }
                        }
                        CheckoutControllerExampleViewModel.Status.Configured -> {
                            ConfirmationControls(
                                paymentOption = session?.paymentOptionDisplayData,
                                confirmationMessage = confirmationMessage,
                                isUpdating = isUpdating,
                                displayMandate = viewModel.displayMandate,
                                onClearPaymentMethod = viewModel::clearPaymentOption,
                                onSelectPaymentMethod = paymentElement::present,
                                onSetShippingAddress = shippingAddressElement::present,
                                onConfirm = {
                                    viewModel.clearConfirmationMessage()
                                    presenter.confirm()
                                },
                            )
                        }
                        else -> Unit
                    }
                },
            )
        }
    }
}
