package com.stripe.android.crypto.onramp.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.stripe.android.core.model.CountryCode
import com.stripe.android.crypto.onramp.example.COLLECT_KYC_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.KYC_ADDRESS_CITY_TAG
import com.stripe.android.crypto.onramp.example.KYC_ADDRESS_COUNTRY_TAG
import com.stripe.android.crypto.onramp.example.KYC_ADDRESS_LINE_1_TAG
import com.stripe.android.crypto.onramp.example.KYC_ADDRESS_POSTAL_CODE_TAG
import com.stripe.android.crypto.onramp.example.KYC_ADDRESS_STATE_TAG
import com.stripe.android.crypto.onramp.example.KYC_BIRTH_CITY_TAG
import com.stripe.android.crypto.onramp.example.KYC_BIRTH_COUNTRY_TAG
import com.stripe.android.crypto.onramp.example.KYC_FIRST_NAME_TAG
import com.stripe.android.crypto.onramp.example.KYC_ID_NUMBER_TAG
import com.stripe.android.crypto.onramp.example.KYC_LAST_NAME_TAG
import com.stripe.android.crypto.onramp.example.KYC_NATIONALITIES_TAG
import com.stripe.android.crypto.onramp.example.KYC_RESIDENCE_DROPDOWN_TAG
import com.stripe.android.crypto.onramp.example.KYC_SECTION_TAG
import com.stripe.android.crypto.onramp.example.VERIFY_KYC_BUTTON_TAG
import com.stripe.android.crypto.onramp.example.model.KycResidence
import com.stripe.android.crypto.onramp.model.IdType
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
    residence: KycResidence,
    onResidenceChange: (KycResidence) -> Unit,
    address: PaymentSheet.Address,
    onAddressChange: (PaymentSheet.Address) -> Unit,
    onCollectKyc: (KycInfo) -> Unit,
    onVerifyKyc: () -> Unit
) {
    Row(
        modifier = Modifier
            .testTag(KYC_SECTION_TAG)
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
                residence = residence,
                onResidenceChange = onResidenceChange,
                address = address,
                onAddressChange = onAddressChange,
                onCollectKyc = onCollectKyc
            )

            Button(
                onClick = onVerifyKyc,
                modifier = Modifier
                    .testTag(VERIFY_KYC_BUTTON_TAG)
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
    residence: KycResidence,
    onResidenceChange: (KycResidence) -> Unit,
    address: PaymentSheet.Address,
    onAddressChange: (PaymentSheet.Address) -> Unit,
    onCollectKyc: (KycInfo) -> Unit
) {
    var isResidenceDropdownExpanded by remember { mutableStateOf(false) }
    var idNumber by remember {
        mutableStateOf(residence.nationalIdConfiguration?.defaultValue.orEmpty())
    }
    var dobDay by remember { mutableStateOf(DEFAULT_DOB_DAY) }
    var dobMonth by remember { mutableStateOf(DEFAULT_DOB_MONTH) }
    var dobYear by remember { mutableStateOf(DEFAULT_DOB_YEAR) }
    val nationalIdConfiguration = residence.nationalIdConfiguration

    Column {
        Text(
            text = "Collect KYC Info",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box {
            OutlinedTextField(
                value = residence.displayName,
                onValueChange = { },
                readOnly = true,
                label = { Text("Residence") },
                trailingIcon = {
                    TextButton(
                        onClick = { isResidenceDropdownExpanded = true },
                        modifier = Modifier.testTag(KYC_RESIDENCE_DROPDOWN_TAG),
                    ) {
                        Text("▼")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            DropdownMenu(
                expanded = isResidenceDropdownExpanded,
                onDismissRequest = { isResidenceDropdownExpanded = false },
            ) {
                KycResidence.entries.forEach { selectedResidence ->
                    DropdownMenuItem(
                        onClick = {
                            onResidenceChange(selectedResidence)
                            idNumber = selectedResidence.nationalIdConfiguration?.defaultValue.orEmpty()
                            isResidenceDropdownExpanded = false
                        }
                    ) {
                        Text(selectedResidence.displayName)
                    }
                }
            }
        }

        KycTextField(
            value = firstName,
            label = "First Name",
            modifier = Modifier
                .fillMaxWidth()
                .testTag(KYC_FIRST_NAME_TAG),
            onChange = onFirstNameChange
        )
        KycTextField(
            value = lastName,
            label = "Last Name",
            modifier = Modifier
                .fillMaxWidth()
                .testTag(KYC_LAST_NAME_TAG),
            onChange = onLastNameChange
        )
        if (nationalIdConfiguration != null) {
            KycTextField(
                value = idNumber,
                label = nationalIdConfiguration.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(KYC_ID_NUMBER_TAG),
                keyboardType = KeyboardType.Ascii,
                onChange = { idNumber = it }
            )
        }

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

        if (residence.followsEuFlow) {
            Text(
                text = "Birth Details",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )

            KycTextField(
                value = birthCountry,
                label = "Birth Country (ISO)",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(KYC_BIRTH_COUNTRY_TAG),
                onChange = onBirthCountryChange
            )
            KycTextField(
                value = birthCity,
                label = "Birth City",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(KYC_BIRTH_CITY_TAG),
                onChange = onBirthCityChange
            )
            KycTextField(
                value = nationalities,
                label = "Nationalities (ISO, comma-separated)",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(KYC_NATIONALITIES_TAG),
                onChange = onNationalitiesChange
            )
        }

        Text(
            text = "Address",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        KycTextField(
            value = address.line1.orEmpty(),
            label = "Address Line 1",
            modifier = Modifier
                .fillMaxWidth()
                .testTag(KYC_ADDRESS_LINE_1_TAG),
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag(KYC_ADDRESS_CITY_TAG),
            onChange = { onAddressChange(address.replacing(city = it)) }
        )
        KycTextField(
            value = address.state.orEmpty(),
            label = if (residence.requiresState) "State/Province *" else "State/Province",
            modifier = Modifier
                .fillMaxWidth()
                .testTag(KYC_ADDRESS_STATE_TAG),
            onChange = { onAddressChange(address.replacing(state = it)) }
        )
        KycTextField(
            value = address.country.orEmpty(),
            label = "Country",
            modifier = Modifier
                .fillMaxWidth()
                .testTag(KYC_ADDRESS_COUNTRY_TAG),
            onChange = { onAddressChange(address.replacing(country = it)) }
        )
        KycTextField(
            value = address.postalCode.orEmpty(),
            label = "Postal Code",
            modifier = Modifier
                .fillMaxWidth()
                .testTag(KYC_ADDRESS_POSTAL_CODE_TAG),
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
                        idNumber = nationalIdConfiguration?.let {
                            idNumber.trim().takeIf(String::isNotEmpty)
                        },
                        idType = nationalIdConfiguration?.type ?: IdType.SocialSecurityNumber,
                        dateOfBirth = dateOfBirth,
                        address = address,
                        birthCountry = if (residence.followsEuFlow) {
                            birthCountry.toCountryCodeOrNull()
                        } else {
                            null
                        },
                        birthCity = if (residence.followsEuFlow) {
                            birthCity.trim().takeIf(String::isNotEmpty)
                        } else {
                            null
                        },
                        nationalities = if (residence.followsEuFlow) {
                            nationalities.toCountryCodesOrNull()
                        } else {
                            null
                        }
                    )
                )
            },
            modifier = Modifier
                .testTag(COLLECT_KYC_BUTTON_TAG)
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

private const val DEFAULT_DOB_DAY = "1"
private const val DEFAULT_DOB_MONTH = "1"
private const val DEFAULT_DOB_YEAR = "1990"
