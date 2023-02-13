package com.stripe.android.identity.ui

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.DobParam
import com.stripe.android.identity.networking.models.IdNumberParam
import com.stripe.android.identity.networking.models.NameParam
import com.stripe.android.identity.networking.models.RequiredInternationalAddress
import com.stripe.android.identity.networking.models.Requirement
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
    isStandalone: Boolean,
    identityViewModel: IdentityViewModel
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current
    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(context)
        }
    ) { verificationPage ->
        val missing by remember {
            mutableStateOf(identityViewModel.missingRequirements.value)
        }
        val individualPage = requireNotNull(verificationPage.individual)
        val coroutineScope = rememberCoroutineScope()

        var collectedName: Resource<NameParam> by remember {
            mutableStateOf(
                requirementResource(missing, Requirement.NAME)
            )
        }

        var collectedDob: Resource<DobParam> by remember {
            mutableStateOf(
                requirementResource(missing, Requirement.DOB)
            )
        }

        var collectedIdNumber: Resource<IdNumberParam> by remember {
            mutableStateOf(
                requirementResource(missing, Requirement.IDNUMBER)
            )
        }

        var collectedAddress: Resource<RequiredInternationalAddress> by remember {
            mutableStateOf(
                requirementResource(missing, Requirement.ADDRESS)
            )
        }

        var submitButtonState: LoadingButtonState by remember {
            mutableStateOf(LoadingButtonState.Disabled)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = dimensionResource(id = R.dimen.page_horizontal_margin),
                    end = dimensionResource(id = R.dimen.page_horizontal_margin),
                    top = dimensionResource(id = R.dimen.page_vertical_margin),
                    bottom = dimensionResource(id = R.dimen.page_vertical_margin)
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = individualPage.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag(INDIVIDUAL_TITLE_TAG)
                )
                if (missing.contains(Requirement.NAME)) {
                    NameSection {
                        collectedName = it
                        submitButtonState = updateSubmitButtonState(
                            listOf(
                                collectedDob,
                                collectedAddress,
                                collectedName,
                                collectedIdNumber
                            )
                        )
                    }
                }
                if (missing.contains(Requirement.DOB)) {
                    DOBSection {
                        collectedDob = it
                        submitButtonState = updateSubmitButtonState(
                            listOf(
                                collectedDob,
                                collectedAddress,
                                collectedName,
                                collectedIdNumber
                            )
                        )
                    }
                }
                if (missing.contains(Requirement.IDNUMBER)) {
                    IDNumberSection(
                        individualPage.idNumberCountries,
                        individualPage.idNumberCountryNotListedTextButtonText,
                        isStandalone,
                        navController
                    ) {
                        collectedIdNumber = it

                        submitButtonState = updateSubmitButtonState(
                            listOf(
                                collectedDob,
                                collectedAddress,
                                collectedName,
                                collectedIdNumber
                            )
                        )
                    }
                }
                if (missing.contains(Requirement.ADDRESS)) {
                    AddressSection(
                        identityViewModel,
                        individualPage.addressCountries,
                        individualPage.addressCountryNotListedTextButtonText,
                        isStandalone,
                        navController
                    ) {
                        collectedAddress = it
                        submitButtonState = updateSubmitButtonState(
                            listOf(
                                collectedDob,
                                collectedAddress,
                                collectedName,
                                collectedIdNumber
                            )
                        )
                    }
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
                        collectedName,
                        collectedDob,
                        collectedIdNumber,
                        collectedAddress,
                        navController
                    )
                }
            }
        }
    }
}

// Disable submit button if there is still status loading or error
private fun updateSubmitButtonState(requirements: List<Resource<Parcelable>>) =
    requirements.firstOrNull {
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
