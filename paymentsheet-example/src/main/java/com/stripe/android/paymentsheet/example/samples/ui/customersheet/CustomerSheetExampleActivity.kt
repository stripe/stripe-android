package com.stripe.android.paymentsheet.example.samples.ui.customersheet

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
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
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.example.utils.rememberDrawablePainter

@OptIn(ExperimentalCustomerSheetApi::class)
internal class CustomerSheetExampleActivity : AppCompatActivity() {
    private val viewModel by viewModels<CustomerSheetExampleViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = getString(R.string.customer_toolbar_title)

        setContent {
            PaymentSheetExampleTheme {
                val customerSheet = rememberCustomerSheet(
                    customerAdapter = viewModel.customerAdapter,
                    configuration = buildConfig(),
                    callback = viewModel::onCustomerSheetResult,
                )

                val viewState by viewModel.state.collectAsState()
                val isDeveloperModeEnabled by viewModel.isDeveloperModeEnabled.collectAsState()
                val isSetupIntentEnabled by viewModel.isSetupIntentEnabled.collectAsState()

                LaunchedEffect(Unit) {
                    val result = customerSheet.retrievePaymentOptionSelection()
                    viewModel.onCustomerSheetResult(result)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    AnimatedVisibility(visible = isDeveloperModeEnabled) {
                        DeveloperConfigurations(
                            isSetupIntentEnabled = isSetupIntentEnabled,
                            toggleSetupIntentEnabled = viewModel::toggleSetupIntentEnabled,
                        )
                    }

                    Text(
                        text = "Payment Methods",
                        fontSize = 18.sp
                    )

                    when (val state = viewState) {
                        is CustomerSheetExampleViewState.Data -> {
                            CustomerPaymentMethods(
                                state = state,
                                onUpdateDefaultPaymentMethod = {
                                    customerSheet.present()
                                }
                            )
                        }
                        is CustomerSheetExampleViewState.FailedToLoad -> {
                            Text(
                                text = state.message
                            )
                        }
                        is CustomerSheetExampleViewState.Loading -> {
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_customersheet, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.developer_mode) {
            viewModel.toggleDeveloperMode()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun buildConfig(): CustomerSheet.Configuration {
        return CustomerSheet.Configuration.Builder()
            .defaultBillingDetails(
                PaymentSheet.BillingDetails(
                    name = "CustomerSheet Testing"
                )
            ).billingDetailsCollectionConfiguration(
                PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                )
            )
            .googlePayEnabled(true)
            .build()
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
@Composable
private fun CustomerPaymentMethods(
    state: CustomerSheetExampleViewState.Data,
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
fun DeveloperConfigurations(
    isSetupIntentEnabled: Boolean,
    toggleSetupIntentEnabled: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Developer Configurations",
            fontSize = 18.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isSetupIntentEnabled) {
                    "SetupIntent"
                } else {
                    "CreateAndAttach"
                }
            )
            Switch(
                checked = isSetupIntentEnabled,
                onCheckedChange = {
                    toggleSetupIntentEnabled(it)
                }
            )
        }
    }
}
