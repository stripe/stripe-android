package com.stripe.android.paymentsheet.example.samples.ui.saved_pms

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.customer.CustomerAdapterConfig
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme
import com.stripe.android.paymentsheet.wallet.controller.SavedPaymentMethodsController
import com.stripe.android.paymentsheet.wallet.embeddable.SavedPaymentMethods
import com.stripe.android.paymentsheet.wallet.sheet.SavedPaymentMethodsSheet

internal class SavedPaymentMethodsActivity : AppCompatActivity() {
    private val viewModel by viewModels<SavedPaymentMethodsViewModel>()

    private lateinit var savedPaymentMethodsSheet: SavedPaymentMethodsSheet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedPaymentMethodsSheet = SavedPaymentMethodsSheet.create(
            activity = this,
            callback = {

            }
        )

        setContent {
            PaymentSheetExampleTheme {
                val uiState by viewModel.state.collectAsState()

                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Customer type",
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        Text("New")
                        RadioButton(
                            selected = uiState.customerType ==
                                SavedPaymentMethodsViewState.CustomerType.New,
                            onClick = {
                                viewModel.updateCustomerType(
                                    SavedPaymentMethodsViewState.CustomerType.New
                                )
                            }
                        )
                        Text("Returning")
                        RadioButton(
                            selected = uiState.customerType ==
                                SavedPaymentMethodsViewState.CustomerType.Returning,
                            onClick = {
                                viewModel.updateCustomerType(
                                    SavedPaymentMethodsViewState.CustomerType.Returning
                                )
                            }
                        )
                    }
                    uiState.customerState?.let { state ->
                        Column {
                            Row {
                                TextButton(
                                    onClick = {
                                        savedPaymentMethodsSheet.presentSavedPaymentMethods(
                                            configuration = SavedPaymentMethodsSheet.Configuration(
                                                customerId = state.customerId,
                                                customerEphemeralKeyProvider = {
                                                    viewModel.fetchEphemeralKey()
                                                },
                                                merchantDisplayName = "Test",
                                            )
                                        )
                                    }
                                ) {
                                    Text("Sheet Prototype Payment method")
                                }
                            }
                        }
                    } ?: run {
                        Text("This customer has no payment methods")
                    }
                }
            }
        }
    }
}
