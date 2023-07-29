package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.DobParam
import com.stripe.android.identity.networking.models.IdNumberParam
import com.stripe.android.identity.networking.models.NameParam
import com.stripe.android.identity.networking.models.PhoneParam
import com.stripe.android.identity.networking.models.RequiredInternationalAddress
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPageStaticContentIndividualPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * Screen to collect the following missing individual information:
 *  [Requirement.DOB], [Requirement.NAME], [Requirement.IDNUMBER], [Requirement.ADDRESS].
 *
 * When verification type is document, [Requirement.DOB], [Requirement.NAME] is not required,
 * Individual screen will be shown after document and selfie are collected.
 *
 * When verification type is not document, [Requirement.DOB], [Requirement.NAME] is required,
 * Individual screen will be shown as a standalone screen to collect user information.
 */
@Composable
internal fun IndividualScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel
) {
    CheckVerificationPageAndCompose(
        identityViewModel = identityViewModel,
        navController = navController
    ) { verificationPage ->
        val missing by remember {
            mutableStateOf(identityViewModel.missingRequirements.value)
        }

        val individualPage = requireNotNull(verificationPage.individual)
        val coroutineScope = rememberCoroutineScope()
        val collectedStates: IndividualCollectedStates by remember {
            mutableStateOf(
                IndividualCollectedStates(
                    name = requirementResource(missing, Requirement.NAME),
                    dob = requirementResource(missing, Requirement.DOB),
                    idNumber = requirementResource(missing, Requirement.IDNUMBER),
                    address = requirementResource(missing, Requirement.ADDRESS),
                    phone = requirementResource(missing, Requirement.PHONE_NUMBER)
                )
            )
        }
        var submitButtonState: LoadingButtonState by remember {
            mutableStateOf(LoadingButtonState.Disabled)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                    end = dimensionResource(id = R.dimen.stripe_page_horizontal_margin),
                    top = dimensionResource(id = R.dimen.stripe_page_vertical_margin),
                    bottom = dimensionResource(id = R.dimen.stripe_page_vertical_margin)
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                IndividualScreenBodyContent(
                    enabled = submitButtonState != LoadingButtonState.Loading,
                    navController = navController,
                    identityViewModel = identityViewModel,
                    individualPage = individualPage,
                    missing = missing,
                    collectedStates = collectedStates,
                ) { newLoadingButtonState ->
                    submitButtonState = newLoadingButtonState
                }
            }

            LoadingButton(
                modifier = Modifier.testTag(INDIVIDUAL_SUBMIT_BUTTON_TAG),
                text = individualPage.buttonText.uppercase(),
                state = submitButtonState
            ) {
                submitButtonState = LoadingButtonState.Loading
                // submit with collected states
                // for all states that's not Idle
                coroutineScope.launch {
                    identityViewModel.postVerificationPageDataForIndividual(
                        collectedStates,
                        navController
                    )
                }
            }
        }
    }
}

@Composable
private fun IndividualScreenBodyContent(
    enabled: Boolean,
    navController: NavController,
    identityViewModel: IdentityViewModel,
    individualPage: VerificationPageStaticContentIndividualPage,
    missing: Set<Requirement>,
    collectedStates: IndividualCollectedStates,
    onUpdateLoadingButtonState: (LoadingButtonState) -> Unit
) {
    Text(
        text = individualPage.title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag(INDIVIDUAL_TITLE_TAG)
    )
    if (missing.contains(Requirement.PHONE_NUMBER)) {
        PhoneNumberSection(
            enabled,
            individualPage.phoneNumberCountries,
        ) {
            collectedStates.phone = it
            onUpdateLoadingButtonState(
                updateSubmitButtonState(
                    collectedStates
                )
            )
        }
    }
    if (missing.contains(Requirement.NAME)) {
        NameSection(enabled) {
            collectedStates.name = it
            onUpdateLoadingButtonState(
                updateSubmitButtonState(
                    collectedStates
                )
            )
        }
    }
    if (missing.contains(Requirement.DOB)) {
        DOBSection(enabled) {
            collectedStates.dob = it
            onUpdateLoadingButtonState(
                updateSubmitButtonState(
                    collectedStates
                )
            )
        }
    }
    if (missing.contains(Requirement.IDNUMBER)) {
        IDNumberSection(
            enabled,
            individualPage.idNumberCountries,
            individualPage.idNumberCountryNotListedTextButtonText,
            navController
        ) {
            collectedStates.idNumber = it
            onUpdateLoadingButtonState(
                updateSubmitButtonState(
                    collectedStates
                )
            )
        }
    }
    if (missing.contains(Requirement.ADDRESS)) {
        AddressSection(
            enabled,
            identityViewModel,
            individualPage.addressCountries,
            individualPage.addressCountryNotListedTextButtonText,
            navController
        ) {
            collectedStates.address = it
            onUpdateLoadingButtonState(
                updateSubmitButtonState(
                    collectedStates
                )
            )
        }
    }
}

internal data class IndividualCollectedStates(
    var name: Resource<NameParam>,
    var dob: Resource<DobParam>,
    var idNumber: Resource<IdNumberParam>,
    var address: Resource<RequiredInternationalAddress>,
    var phone: Resource<PhoneParam>
) {
    fun allStates() = listOf(name, dob, idNumber, address, phone)
    fun toCollectedDataParam() = CollectedDataParam(
        name = if (name.status == Status.SUCCESS) name.data else null,
        dob = if (dob.status == Status.SUCCESS) dob.data else null,
        idNumber = if (idNumber.status == Status.SUCCESS) idNumber.data else null,
        address = if (address.status == Status.SUCCESS) address.data else null,
        phone = if (phone.status == Status.SUCCESS) phone.data else null,
    )
}

// Disable submit button if there is still status loading or error
private fun updateSubmitButtonState(collectedStates: IndividualCollectedStates) =
    collectedStates.allStates().firstOrNull {
        it.status == Status.LOADING || it.status == Status.ERROR
    }?.let {
        LoadingButtonState.Disabled
    } ?: run {
        LoadingButtonState.Idle
    }

private fun <T> requirementResource(
    missingRequirements: Set<Requirement>,
    requirement: Requirement
) = if (missingRequirements.contains(requirement)) {
    Resource.loading<T>()
} else {
    Resource.idle()
}

internal const val INDIVIDUAL_TITLE_TAG = "IndividualTitle"
internal const val INDIVIDUAL_SUBMIT_BUTTON_TAG = "IndividualSubmitButton"
