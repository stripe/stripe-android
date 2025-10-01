package com.stripe.android.connect.example.ui.settings

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.connect.PaymentsProps
import com.stripe.android.connect.PreviewConnectSDK
import com.stripe.android.connect.example.R
import com.stripe.android.connect.example.data.AmountFilterType
import com.stripe.android.connect.example.data.DateFilterType
import com.stripe.android.connect.example.data.PaymentsSettings
import com.stripe.android.connect.example.ui.common.BackIconButton
import com.stripe.android.connect.example.ui.common.ConnectExampleScaffold
import com.stripe.android.connect.example.ui.common.ConnectSdkExampleTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PaymentsSettingsView(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val paymentsSettings = state.paymentsSettings
    PaymentsSettingsView(
        paymentsSettings = paymentsSettings,
        onBack = onBack,
        onSave = { viewModel.onPaymentsSettingsConfirmed(it) },
    )
}

@Suppress("LongMethod")
@OptIn(PreviewConnectSDK::class)
@Composable
private fun PaymentsSettingsView(
    paymentsSettings: PaymentsSettings,
    onBack: () -> Unit,
    onSave: (PaymentsSettings) -> Unit,
) {
    var amountFilterType by rememberSaveable {
        mutableStateOf(paymentsSettings.amountFilterType)
    }
    var amountValue by rememberSaveable {
        mutableStateOf(paymentsSettings.amountValue.toString())
    }
    var amountLowerBound by rememberSaveable {
        mutableStateOf(paymentsSettings.amountLowerBound.toString())
    }
    var amountUpperBound by rememberSaveable {
        mutableStateOf(paymentsSettings.amountUpperBound.toString())
    }
    var statusFilters by rememberSaveable {
        mutableStateOf(paymentsSettings.statusFilters)
    }
    var paymentMethodFilter by rememberSaveable {
        mutableStateOf(paymentsSettings.paymentMethodFilter)
    }
    var dateFilterType by rememberSaveable {
        mutableStateOf(paymentsSettings.dateFilterType)
    }
    var dateValue by rememberSaveable {
        mutableStateOf(paymentsSettings.dateValue)
    }
    var dateStart by rememberSaveable {
        mutableStateOf(paymentsSettings.dateStart)
    }
    var dateEnd by rememberSaveable {
        mutableStateOf(paymentsSettings.dateEnd)
    }

    ConnectExampleScaffold(
        title = "Payments Settings",
        navigationIcon = { BackIconButton(onBack) },
        actions = {
            IconButton(
                onClick = {
                    onSave(
                        PaymentsSettings(
                            amountFilterType = amountFilterType,
                            amountValue = amountValue.toDoubleOrNull() ?: 0.0,
                            amountLowerBound = amountLowerBound.toDoubleOrNull() ?: 0.0,
                            amountUpperBound = amountUpperBound.toDoubleOrNull() ?: 100.0,
                            statusFilters = statusFilters,
                            paymentMethodFilter = paymentMethodFilter,
                            dateFilterType = dateFilterType,
                            dateValue = dateValue,
                            dateStart = dateStart,
                            dateEnd = dateEnd,
                        )
                    )
                    onBack()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.save)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.requiredHeight(16.dp))

            SettingsDropdownField(
                label = "Amount Filter Type",
                options = AmountFilterType.entries.toList(),
                selectedOption = amountFilterType,
                onSelectOption = { amountFilterType = it }
            )

            Spacer(Modifier.requiredHeight(8.dp))

            when (amountFilterType) {
                AmountFilterType.EQUALS, AmountFilterType.GREATER_THAN, AmountFilterType.LESS_THAN -> {
                    SettingsTextField(
                        label = "Amount Value",
                        placeholder = "0.00",
                        value = amountValue,
                        onValueChange = { amountValue = it },
                    )
                }
                AmountFilterType.BETWEEN -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SettingsTextField(
                            label = "Lower Bound",
                            placeholder = "0.00",
                            value = amountLowerBound,
                            onValueChange = { amountLowerBound = it },
                        )
                        Spacer(modifier = Modifier.requiredHeight(8.dp))
                        SettingsTextField(
                            label = "Upper Bound",
                            placeholder = "100.00",
                            value = amountUpperBound,
                            onValueChange = { amountUpperBound = it },
                        )
                    }
                }
                AmountFilterType.NONE -> {
                    // No additional fields needed
                }
            }

            Spacer(Modifier.requiredHeight(8.dp))

            SettingsMultiSelectField(
                label = "Status Filters",
                options = PaymentsProps.Status.entries.toList(),
                selectedOptions = statusFilters,
                onSelectedOptionsChange = { statusFilters = it },
                optionToString = { status ->
                    status.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
                }
            )

            Spacer(Modifier.requiredHeight(8.dp))

            SettingsDropdownField(
                label = "Payment Method Filter",
                options = listOf<PaymentsProps.PaymentMethod?>(null) + PaymentsProps.PaymentMethod.entries,
                selectedOption = paymentMethodFilter,
                onSelectOption = { paymentMethodFilter = it },
                optionToString = { option ->
                    option?.name?.lowercase()?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "NONE"
                }
            )

            Spacer(Modifier.requiredHeight(8.dp))

            SettingsDropdownField(
                label = "Date Filter Type",
                options = DateFilterType.entries.toList(),
                selectedOption = dateFilterType,
                onSelectOption = { dateFilterType = it }
            )

            Spacer(Modifier.requiredHeight(8.dp))

            when (dateFilterType) {
                DateFilterType.BEFORE, DateFilterType.AFTER -> {
                    DatePickerField(
                        label = "Date Value",
                        selectedTimestamp = dateValue,
                        onDateSelected = { dateValue = it },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                DateFilterType.BETWEEN -> {
                    Column {
                        DatePickerField(
                            label = "Start Date",
                            selectedTimestamp = dateStart,
                            onDateSelected = { dateStart = it },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.requiredHeight(8.dp))
                        DatePickerField(
                            label = "End Date",
                            selectedTimestamp = dateEnd,
                            onDateSelected = { dateEnd = it },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                DateFilterType.NONE -> {
                    // No additional fields needed
                }
            }

            Spacer(Modifier.requiredHeight(24.dp))

            // Reset button
            Button(
                onClick = {
                    amountFilterType = AmountFilterType.NONE
                    amountValue = "0.0"
                    amountLowerBound = "0.0"
                    amountUpperBound = "100.0"
                    statusFilters = emptyList()
                    paymentMethodFilter = null
                    dateFilterType = DateFilterType.NONE
                    dateValue = ""
                    dateStart = ""
                    dateEnd = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colors.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Reset to Default")
            }

            Spacer(Modifier.requiredHeight(16.dp))
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    selectedTimestamp: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val displayDate = if (selectedTimestamp.isNotEmpty()) {
        try {
            dateFormatter.format(Date(selectedTimestamp.toLong()))
        } catch (e: NumberFormatException) {
            "Select date"
        }
    } else {
        "Select date"
    }

    // Get initial date from timestamp or use current date
    val initialDate = if (selectedTimestamp.isNotEmpty()) {
        try {
            Date(selectedTimestamp.toLong())
        } catch (e: NumberFormatException) {
            Date()
        }
    } else {
        Date()
    }

    val calendar = remember { Calendar.getInstance() }
    calendar.time = initialDate

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        onDateSelected(calendar.timeInMillis.toString())
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = displayDate)
        }
    }
}

@Preview
@Composable
@OptIn(PreviewConnectSDK::class)
private fun PaymentsSettingsViewPreview() {
    ConnectSdkExampleTheme {
        PaymentsSettingsView(
            paymentsSettings = PaymentsSettings(),
            onBack = {},
            onSave = {}
        )
    }
}
