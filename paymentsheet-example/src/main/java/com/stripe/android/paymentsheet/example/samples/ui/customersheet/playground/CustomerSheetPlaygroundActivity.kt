package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

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
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.shared.MultiToggleButton
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.utils.rememberDrawablePainter
import java.util.Locale

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetPlaygroundActivity : AppCompatActivity() {

    private val viewModel by viewModels<CustomerSheetPlaygroundViewModel> {
        CustomerSheetPlaygroundViewModel.Factory
    }

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = getString(R.string.customersheet_playground_title)

        setContent {
            PaymentSheetExampleTheme {
                val viewState by viewModel.viewState.collectAsState()
                val configurationState by viewModel.configurationState.collectAsState()
                val config by viewModel.configuration.collectAsState()
                val customerAdapter by viewModel.customerAdapter.collectAsState()

                val currentCustomerAdapter = customerAdapter ?: return@PaymentSheetExampleTheme

                val customerSheet = rememberCustomerSheet(
                    customerAdapter = currentCustomerAdapter,
                    configuration = config,
                    callback = viewModel::onCustomerSheetResult,
                )

                LaunchedEffect(configurationState.isExistingCustomer) {
                    val result = customerSheet.retrievePaymentOptionSelection()
                    viewModel.onCustomerSheetResult(result)
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

                    Text(
                        text = "Payment Methods",
                        color = MaterialTheme.colors.onBackground,
                        fontSize = 18.sp,
                    )

                    when (val state = viewState) {
                        is CustomerSheetPlaygroundViewState.Data -> {
                            CustomerPaymentMethods(
                                state = state,
                                onUpdateDefaultPaymentMethod = {
                                    customerSheet.present()
                                }
                            )
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
                Text(
                    "Payment default",
                    color = MaterialTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold,
                )
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
        var collapsed by rememberSaveable { mutableStateOf(false) }
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
                            collapsed = !collapsed
                        }
                )
            }

            AnimatedVisibility(visible = !collapsed) {
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
                    BillingDetailsConfiguration(
                        configurationState = configurationState,
                        viewActionHandler = viewActionHandler,
                    )
                }
            }
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
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
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
        }
    }
}
