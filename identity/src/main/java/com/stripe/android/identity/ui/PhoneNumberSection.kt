package com.stripe.android.identity.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.stripe.android.core.model.Country
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.PhoneParam
import com.stripe.android.uicore.elements.PhoneNumberCollectionSection
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.utils.collectAsState

@Composable
internal fun PhoneNumberSection(
    enabled: Boolean,
    phoneNumberCountries: List<Country>,
    onPhoneCollected: (Resource<PhoneParam>) -> Unit
) {
    val phoneNumberController = remember {
        PhoneNumberController.createPhoneNumberController(
            overrideCountryCodes = phoneNumberCountries.map { it.code.value }.toSet()
        )
    }

    val currentPhoneNumber by phoneNumberController.rawFieldValue.collectAsState()
    val isComplete by phoneNumberController.isComplete.collectAsState()

    val phoneParam: PhoneParam? by remember(isComplete) {
        derivedStateOf {
            if (isComplete) {
                PhoneParam(
                    country = phoneNumberController.getCountryCode(),
                    phoneNumber = currentPhoneNumber
                )
            } else {
                null
            }
        }
    }

    PhoneNumberCollectionSection(
        enabled = enabled,
        phoneNumberController = phoneNumberController,
        sectionTitle = R.string.stripe_phone_number
    )

    LaunchedEffect(phoneParam) {
        onPhoneCollected(
            phoneParam?.let {
                Resource.success(it)
            } ?: run {
                Resource.error()
            }
        )
    }
}
