package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground.CustomerSheetPlaygroundViewAction.UpdateMerchantCountryCode
import com.stripe.android.paymentsheet.example.samples.ui.shared.MultiToggleButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.utils.rememberDrawablePainter
import com.stripe.android.paymentsheet.rememberPaymentSheet
import java.util.Locale

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetPlaygroundActivity : AppCompatActivity() {

    private val viewModel by viewModels<CustomerSheetPlaygroundViewModel> {
        CustomerSheetPlaygroundViewModel.Factory
    }

    private val preferences by lazy {
        getPreferences(Context.MODE_PRIVATE)
    }

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = getString(R.string.customersheet_playground_title)

        setContent {
            PaymentSheetExampleTheme {
                val viewState by viewModel.viewState.collectAsState()
                val paymentResult by viewModel.paymentResult.collectAsState()
                val configurationState by viewModel.configurationState.collectAsState()
                val config by viewModel.configuration.collectAsState()
                val customerAdapter by viewModel.customerAdapter.collectAsState()

                val customerSheet = customerAdapter?.let {
                    rememberCustomerSheet(
                        customerAdapter = it,
                        configuration = config,
                        callback = viewModel::onCustomerSheetResult,
                    )
                }

                val paymentSheet = rememberPaymentSheet(
                    paymentResultCallback = viewModel::handlePaymentResult
                )

                LaunchedEffect(customerAdapter) {
                    customerSheet?.retrievePaymentOptionSelection()?.let {
                        viewModel.onCustomerSheetResult(it)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    DeveloperConfigurations(
                        configurationState = configurationState,
                        viewActionHandler = viewModel::handleViewAction,
                    )

                    Divider(
                        modifier = Modifier.fillMaxWidth()
                    )

                    when (val state = viewState) {
                        is CustomerSheetPlaygroundViewState.Data -> {
                            CustomerPaymentMethods(
                                state = state,
                                onUpdateDefaultPaymentMethod = {
                                    customerSheet?.present()
                                }
                            )
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.clientSecret != null,
                                onClick = {
                                    paymentSheet.presentWithPaymentIntent(
                                        paymentIntentClientSecret = state.clientSecret!!,
                                        configuration = PaymentSheet.Configuration(
                                            merchantDisplayName = "Test Merchant Inc.",
                                            customer = PaymentSheet.CustomerConfiguration(
                                                id = state.currentCustomer!!,
                                                ephemeralKeySecret = state.ephemeralKey!!
                                            )
                                        )
                                    )
                                }
                            ) {
                                Text("Launch PaymentSheet")
                            }
                            paymentResult?.let {
                                Text(it.toString())
                            }
                        }
                        is CustomerSheetPlaygroundViewState.FailedToLoad -> {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colors.onBackground,
                            )
                        }
                        is CustomerSheetPlaygroundViewState.Loading -> {
                            LinearProgressIndicator(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    @Composable
    private fun CustomerPaymentMethods(
        state: CustomerSheetPlaygroundViewState.Data,
        onUpdateDefaultPaymentMethod: () -> Unit
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Selected payment method",
                        color = MaterialTheme.colors.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${state.currentCustomer}",
                        color = MaterialTheme.colors.onBackground,
                    )
                }
                TextButton(
                    onClick = onUpdateDefaultPaymentMethod,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        state.selection?.paymentOption?.icon()?.let {
                            Image(
                                painter = rememberDrawablePainter(
                                    drawable = it
                                ),
                                contentDescription = "Payment Method Icon",
                                modifier = Modifier.height(32.dp)
                            )
                        }
                        Text(
                            text = state.selection?.paymentOption?.label ?: "Select",
                            color = MaterialTheme.colors.onBackground,
                        )
                    }
                }
            }
            state.errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                )
            }
        }
    }

    @Composable
    private fun DeveloperConfigurations(
        configurationState: CustomerSheetPlaygroundConfigurationState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit
    ) {
        var collapsed by rememberSaveable {
            mutableStateOf(preferences.getBoolean(PREFERENCES_DEVELOPER_SETTINGS, false))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Developer Configurations",
                    color = MaterialTheme.colors.onBackground,
                    fontSize = 18.sp
                )
                Icon(
                    imageVector = if (collapsed) {
                        Icons.Rounded.KeyboardArrowRight
                    } else {
                        Icons.Rounded.KeyboardArrowDown
                    },
                    contentDescription = "Developer settings",
                    modifier = Modifier
                        .height(32.dp)
                        .clickable {
                            preferences.edit {
                                putBoolean(PREFERENCES_DEVELOPER_SETTINGS, !collapsed)
                                apply()
                            }
                            collapsed = !collapsed
                        }
                )
            }

            AnimatedVisibility(visible = !collapsed) {
                Switches(
                    configurationState = configurationState,
                    viewActionHandler = viewActionHandler,
                )
            }
        }
    }

    @Composable
    private fun Switches(
        configurationState: CustomerSheetPlaygroundConfigurationState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit
    ) {
        Column {
            SetupIntentSwitch(
                configurationState = configurationState,
                viewActionHandler = viewActionHandler,
            )
            GooglePaySwitch(
                configurationState = configurationState,
                viewActionHandler = viewActionHandler,
            )
            ExistingCustomerSwitch(
                configurationState = configurationState,
                viewActionHandler = viewActionHandler,
            )
            AchEnabledSwitch(
                configurationState = configurationState,
                viewActionHandler = viewActionHandler,
            )
            BillingDetailsConfiguration(
                configurationState = configurationState,
                viewActionHandler = viewActionHandler,
            )
        }
    }

    @Composable
    private fun SetupIntentSwitch(
        configurationState: CustomerSheetPlaygroundConfigurationState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (configurationState.isSetupIntentEnabled) {
                    "SetupIntent"
                } else {
                    "CreateAndAttach"
                },
                color = MaterialTheme.colors.onBackground,
            )
            Switch(
                checked = configurationState.isSetupIntentEnabled,
                onCheckedChange = {
                    viewActionHandler(CustomerSheetPlaygroundViewAction.ToggleSetupIntentEnabled)
                }
            )
        }
    }

    @Composable
    private fun GooglePaySwitch(
        configurationState: CustomerSheetPlaygroundConfigurationState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Google Pay",
                color = MaterialTheme.colors.onBackground,
            )
            Switch(
                checked = configurationState.isGooglePayEnabled,
                onCheckedChange = {
                    viewActionHandler(CustomerSheetPlaygroundViewAction.ToggleGooglePayEnabled)
                }
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ExistingCustomerSwitch(
        configurationState: CustomerSheetPlaygroundConfigurationState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (configurationState.isExistingCustomer) {
                    "Returning customer"
                } else {
                    "New customer"
                },
                color = MaterialTheme.colors.onBackground,
            )
            Switch(
                checked = configurationState.isExistingCustomer,
                onCheckedChange = {
                    viewActionHandler(CustomerSheetPlaygroundViewAction.ToggleExistingCustomer)
                },
                modifier = Modifier
                    .semantics { testTagsAsResourceId = true }
                    .testTag("CUSTOMER_SHEET_PLAYGROUND_EXISTING_CUSTOMER")
            )
        }
    }

    @Composable
    private fun AchEnabledSwitch(
        configurationState: CustomerSheetPlaygroundConfigurationState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "US Bank Accounts",
                color = MaterialTheme.colors.onBackground,
            )
            Switch(
                checked = configurationState.achEnabled,
                onCheckedChange = {
                    viewActionHandler(CustomerSheetPlaygroundViewAction.ToggleAchEnabled)
                },
            )
        }
    }

    @Suppress("LongMethod")
    @Composable
    private fun BillingDetailsConfiguration(
        configurationState: CustomerSheetPlaygroundConfigurationState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Use Default Billing Address",
                    color = MaterialTheme.colors.onBackground,
                )
                Switch(
                    checked = configurationState.useDefaultBillingAddress,
                    onCheckedChange = {
                        viewActionHandler(
                            CustomerSheetPlaygroundViewAction.ToggleUseDefaultBillingAddress
                        )
                    },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Attach Default Billing Address",
                    color = MaterialTheme.colors.onBackground,
                )
                Switch(
                    checked = configurationState.useDefaultBillingAddress,
                    onCheckedChange = {
                        viewActionHandler(
                            CustomerSheetPlaygroundViewAction.ToggleUseDefaultBillingAddress
                        )
                    },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Name",
                    color = MaterialTheme.colors.onBackground,
                )
                MultiToggleButton(
                    currentSelection =
                    configurationState.billingCollectionConfiguration.name.name.lowercase(
                        Locale.getDefault()
                    ),
                    toggleStates = listOf("automatic", "never", "always"),
                    onToggleChange = {
                        viewActionHandler(
                            CustomerSheetPlaygroundViewAction.UpdateBillingNameCollection(it)
                        )
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Email",
                    color = MaterialTheme.colors.onBackground,
                )
                MultiToggleButton(
                    currentSelection =
                    configurationState.billingCollectionConfiguration.email.name.lowercase(
                        Locale.getDefault()
                    ),
                    toggleStates = listOf("automatic", "never", "always"),
                    onToggleChange = {
                        viewActionHandler(
                            CustomerSheetPlaygroundViewAction.UpdateBillingEmailCollection(it)
                        )
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Phone",
                    color = MaterialTheme.colors.onBackground,
                )
                MultiToggleButton(
                    currentSelection =
                    configurationState.billingCollectionConfiguration.phone.name.lowercase(
                        Locale.getDefault()
                    ),
                    toggleStates = listOf("automatic", "never", "always"),
                    onToggleChange = {
                        viewActionHandler(
                            CustomerSheetPlaygroundViewAction.UpdateBillingPhoneCollection(it)
                        )
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Address",
                    color = MaterialTheme.colors.onBackground,
                )
                MultiToggleButton(
                    currentSelection =
                    configurationState.billingCollectionConfiguration.address.name.lowercase(
                        Locale.getDefault()
                    ),
                    toggleStates = listOf("automatic", "never", "full"),
                    onToggleChange = {
                        viewActionHandler(
                            CustomerSheetPlaygroundViewAction.UpdateBillingAddressCollection(it)
                        )
                    }
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "Merchant Country",
                    color = MaterialTheme.colors.onBackground,
                )
                MultiToggleButton(
                    currentSelection = configurationState.merchantCountry,
                    toggleStates = listOf("US", "FR"),
                    onToggleChange = { viewActionHandler(UpdateMerchantCountryCode(it)) },
                )
            }
        }
    }

    companion object {
        private const val PREFERENCES_DEVELOPER_SETTINGS = "PREFERENCES_DEVELOPER_SETTINGS"
    }
}
