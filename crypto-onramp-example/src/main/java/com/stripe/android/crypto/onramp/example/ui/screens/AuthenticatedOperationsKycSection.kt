package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stripe.android.core.model.CountryCode
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.model.DateOfBirth
import com.stripe.android.paymentsheet.PaymentSheet

@Composable
internal fun KycSection(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    birthCountry: String,
    onBirthCountryChange: (String) -> Unit,
    birthCity: String,
    onBirthCityChange: (String) -> Unit,
    nationalities: String,
    onNationalitiesChange: (String) -> Unit,
    address: PaymentSheet.Address,
    onAddressChange: (PaymentSheet.Address) -> Unit,
    onCollectKyc: (KycInfo) -> Unit,
    onVerifyKyc: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandedChange(!isExpanded) }
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "KYC Info",
            fontWeight = FontWeight.Bold
        )
        Text(text = if (isExpanded) "Hide" else "Show")
    }

    AnimatedVisibility(visible = isExpanded) {
        Column {
            KycForm(
                firstName = firstName,
                onFirstNameChange = onFirstNameChange,
                lastName = lastName,
                onLastNameChange = onLastNameChange,
                birthCountry = birthCountry,
                onBirthCountryChange = onBirthCountryChange,
                birthCity = birthCity,
                onBirthCityChange = onBirthCityChange,
                nationalities = nationalities,
                onNationalitiesChange = onNationalitiesChange,
                address = address,
                onAddressChange = onAddressChange,
                onCollectKyc = onCollectKyc
            )

            Button(
                onClick = onVerifyKyc,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Verify KYC Info")
            }
        }
    }
}

@Composable
@Suppress("LongMethod")
private fun KycForm(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    birthCountry: String,
    onBirthCountryChange: (String) -> Unit,
    birthCity: String,
    onBirthCityChange: (String) -> Unit,
    nationalities: String,
    onNationalitiesChange: (String) -> Unit,
    address: PaymentSheet.Address,
    onAddressChange: (PaymentSheet.Address) -> Unit,
    onCollectKyc: (KycInfo) -> Unit
) {
    var ssn by remember { mutableStateOf(DEFAULT_SSN) }
    var dobDay by remember { mutableStateOf(DEFAULT_DOB_DAY) }
    var dobMonth by remember { mutableStateOf(DEFAULT_DOB_MONTH) }
    var dobYear by remember { mutableStateOf(DEFAULT_DOB_YEAR) }

    Column {
        Text(
            text = "Collect KYC Info",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        KycTextField(
            value = firstName,
            label = "First Name",
            onChange = onFirstNameChange
        )
        KycTextField(
            value = lastName,
            label = "Last Name",
            onChange = onLastNameChange
        )
        KycTextField(
            value = ssn,
            label = "SSN",
            keyboardType = KeyboardType.Number,
            onChange = { ssn = it }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KycTextField(
                value = dobMonth,
                label = "Month",
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                onChange = { dobMonth = it }
            )
            KycTextField(
                value = dobDay,
                label = "Day",
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                onChange = { dobDay = it }
            )
            KycTextField(
                value = dobYear,
                label = "Year",
                modifier = Modifier.weight(2f),
                keyboardType = KeyboardType.Number,
                onChange = { dobYear = it }
            )
        }

        Text(
            text = "Birth Details",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        KycTextField(
            value = birthCountry,
            label = "Birth Country (ISO)",
            onChange = onBirthCountryChange
        )
        KycTextField(
            value = birthCity,
            label = "Birth City",
            onChange = onBirthCityChange
        )
        KycTextField(
            value = nationalities,
            label = "Nationalities (ISO, comma-separated)",
            onChange = onNationalitiesChange
        )

        Text(
            text = "Address",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        KycTextField(
            value = address.line1.orEmpty(),
            label = "Address Line 1",
            onChange = { onAddressChange(address.replacing(line1 = it)) }
        )
        KycTextField(
            value = address.line2.orEmpty(),
            label = "Address Line 2",
            onChange = { onAddressChange(address.replacing(line2 = it)) }
        )
        KycTextField(
            value = address.city.orEmpty(),
            label = "City",
            onChange = { onAddressChange(address.replacing(city = it)) }
        )
        KycTextField(
            value = address.state.orEmpty(),
            label = "State",
            onChange = { onAddressChange(address.replacing(state = it)) }
        )
        KycTextField(
            value = address.country.orEmpty(),
            label = "Country",
            onChange = { onAddressChange(address.replacing(country = it)) }
        )
        KycTextField(
            value = address.postalCode.orEmpty(),
            label = "Postal Code",
            onChange = { onAddressChange(address.replacing(postalCode = it)) }
        )

        Button(
            onClick = {
                val dateOfBirth = runCatching {
                    DateOfBirth(
                        day = dobDay.toInt(),
                        month = dobMonth.toInt(),
                        year = dobYear.toInt()
                    )
                }.getOrNull()

                onCollectKyc(
                    KycInfo(
                        firstName = firstName,
                        lastName = lastName,
                        idNumber = ssn,
                        dateOfBirth = dateOfBirth,
                        address = address,
                        birthCountry = birthCountry.toCountryCodeOrNull(),
                        birthCity = birthCity.trim().takeIf { it.isNotEmpty() },
                        nationalities = nationalities.toCountryCodesOrNull()
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text("Collect KYC Info")
        }
    }
}

@Composable
internal fun KycTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next,
            keyboardType = keyboardType
        ),
        modifier = modifier.padding(bottom = 24.dp)
    )
}

private fun String.toCountryCodeOrNull(): CountryCode? {
    return trim()
        .takeIf { it.isNotEmpty() }
        ?.let(CountryCode::create)
}

private fun String.toCountryCodesOrNull(): List<CountryCode>? {
    return split(",")
        .mapNotNull { it.toCountryCodeOrNull() }
        .takeIf { it.isNotEmpty() }
}

private fun PaymentSheet.Address.replacing(
    line1: String? = this.line1,
    line2: String? = this.line2,
    city: String? = this.city,
    state: String? = this.state,
    country: String? = this.country,
    postalCode: String? = this.postalCode,
): PaymentSheet.Address {
    return PaymentSheet.Address(
        city = city,
        country = country,
        line1 = line1,
        line2 = line2,
        postalCode = postalCode,
        state = state
    )
}

private const val DEFAULT_SSN = "000000000"
private const val DEFAULT_DOB_DAY = "1"
private const val DEFAULT_DOB_MONTH = "1"
private const val DEFAULT_DOB_YEAR = "1990"
