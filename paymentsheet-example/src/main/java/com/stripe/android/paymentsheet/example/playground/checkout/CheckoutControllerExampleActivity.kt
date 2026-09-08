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
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundScenarios
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundScenariosUi
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettingsUi
import com.stripe.android.paymentsheet.example.playground.checkout.settings.configurations
import kotlinx.coroutines.launch

internal class CheckoutControllerExampleActivity : AppCompatActivity() {
    private val viewModel: CheckoutControllerExampleViewModel by viewModels {
        CheckoutControllerExampleViewModel.factory
    }

    private val settingsImportExport = SettingsImportExport(this) { viewModel.settings }

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
            var scenarioNavigationPath by rememberSaveable {
                mutableStateOf<List<String>>(emptyList())
            }
            var settingsSearchQuery by rememberSaveable { mutableStateOf("") }
            val isSearching = settingsSearchQuery.isNotBlank()
            val isBrowsingScenarios = scenarioNavigationPath.isNotEmpty()
            val currentConfiguration = CheckoutPlaygroundDefinitions.root.configurations()
                .firstOrNull { it.key == navigationPath.lastOrNull() }
                ?: CheckoutPlaygroundDefinitions.root
            val currentScenarioGroup = CheckoutPlaygroundScenarios.groups
                .firstOrNull { it.key == scenarioNavigationPath.lastOrNull() }
                ?: CheckoutPlaygroundScenarios.root
            val navigateBack = {
                if (status is CheckoutControllerExampleViewModel.Status.Settings) {
                    if (isBrowsingScenarios) {
                        scenarioNavigationPath = scenarioNavigationPath.dropLast(1)
                    } else if (isSearching) {
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
                    navigationPath.isNotEmpty() || isSearching || isBrowsingScenarios
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
                                        isBrowsingScenarios -> currentScenarioGroup.displayName
                                        isSearching -> "Search settings"
                                        else -> currentConfiguration.displayName
                                    }
                                )
                            },
                            navigationIcon = if (
                                status !is CheckoutControllerExampleViewModel.Status.Settings ||
                                navigationPath.isNotEmpty() || isSearching || isBrowsingScenarios
                            ) {
                                {
                                    IconButton(onClick = navigateBack) {
                                        Text("‹", style = MaterialTheme.typography.h4)
                                    }
                                }
                            } else {
                                null
                            },
                            actions = {
                                if (
                                    status is CheckoutControllerExampleViewModel.Status.Settings &&
                                    navigationPath.isEmpty() && !isBrowsingScenarios
                                ) {
                                    SettingsOverflowMenu(
                                        onRunScenario = {
                                            settingsSearchQuery = ""
                                            scenarioNavigationPath = listOf(CheckoutPlaygroundScenarios.root.key)
                                        },
                                        onImport = settingsImportExport::importSettings,
                                        onExport = settingsImportExport::exportSettings,
                                        onReset = viewModel.settings::reset,
                                    )
                                }
                            },
                        )
                        if (
                            status is CheckoutControllerExampleViewModel.Status.Settings &&
                            !isBrowsingScenarios
                        ) {
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
                            if (isBrowsingScenarios) {
                                CheckoutPlaygroundScenariosUi(
                                    group = currentScenarioGroup,
                                    onOpenGroup = { scenarioNavigationPath += it.key },
                                    onSelect = { scenario ->
                                        scenarioNavigationPath = emptyList()
                                        viewModel.settings.applyPreset(scenario.preset)
                                        viewModel.start()
                                    },
                                )
                            } else {
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
                            if (navigationPath.isEmpty() && !isBrowsingScenarios) {
                                SettingsActions(
                                    canStart = settingValues.isNotEmpty() &&
                                        viewModel.settings.validationErrors().isEmpty(),
                                    onStart = viewModel::start,
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
