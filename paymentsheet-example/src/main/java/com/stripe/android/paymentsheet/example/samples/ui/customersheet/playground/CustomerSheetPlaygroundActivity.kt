package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.utils.rememberDrawablePainter

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetPlaygroundActivity : AppCompatActivity() {

    private val viewModel by viewModels<CustomerSheetPlaygroundViewModel> {
        CustomerSheetPlaygroundViewModel.Factory(
            application = this.application,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = getString(R.string.customersheet_playground_title)

        setContent {
            PaymentSheetExampleTheme {
                val viewState by viewModel.viewState.collectAsState()
                val config by viewModel.configuration.collectAsState()
                val customerAdapter by viewModel.customerAdapter.collectAsState()

                val customerSheet = rememberCustomerSheet(
                    customerAdapter = customerAdapter,
                    configuration = config,
                    callback = viewModel::onCustomerSheetResult,
                )

                LaunchedEffect(viewState.isExistingCustomer) {
                    val result = customerSheet.retrievePaymentOptionSelection()
                    viewModel.onCustomerSheetResult(result)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    DeveloperConfigurations(
                        viewState = viewState,
                        viewActionHandler = viewModel::handleViewAction,
                    )

                    Text(
                        text = "Payment Methods",
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
                                text = state.message
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
        viewState: CustomerSheetPlaygroundViewState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = "Developer Configurations",
                fontSize = 18.sp
            )

            SetupIntentSwitch(
                viewState = viewState,
                viewActionHandler = viewActionHandler,
            )
            GooglePaySwitch(
                viewState = viewState,
                viewActionHandler = viewActionHandler,
            )
            ExistingCustomerSwitch(
                viewState = viewState,
                viewActionHandler = viewActionHandler,
            )
        }
    }

    @Composable
    private fun SetupIntentSwitch(
        viewState: CustomerSheetPlaygroundViewState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (viewState.isSetupIntentEnabled) {
                    "SetupIntent"
                } else {
                    "CreateAndAttach"
                }
            )
            Switch(
                checked = viewState.isSetupIntentEnabled,
                onCheckedChange = {
                    viewActionHandler(CustomerSheetPlaygroundViewAction.ToggleSetupIntentEnabled)
                }
            )
        }
    }

    @Composable
    private fun GooglePaySwitch(
        viewState: CustomerSheetPlaygroundViewState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Google Pay"
            )
            Switch(
                checked = viewState.isGooglePayEnabled,
                onCheckedChange = {
                    viewActionHandler(CustomerSheetPlaygroundViewAction.ToggleGooglePayEnabled)
                }
            )
        }
    }

    @Composable
    private fun ExistingCustomerSwitch(
        viewState: CustomerSheetPlaygroundViewState,
        viewActionHandler: (CustomerSheetPlaygroundViewAction) -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (viewState.isExistingCustomer) {
                    "Returning customer"
                } else {
                    "New customer"
                }
            )
            Switch(
                checked = viewState.isExistingCustomer,
                onCheckedChange = {
                    viewActionHandler(CustomerSheetPlaygroundViewAction.ToggleExistingCustomer)
                }
            )
        }
    }
}
