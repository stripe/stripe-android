package com.stripe.android.identity.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.navigation.VerificationWebViewDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.Requirement.Companion.nextDestination
import com.stripe.android.identity.networking.models.VerificationPage.Companion.isUnsupportedClient
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Initial screen with a spinner, to decide which screen to transition based on missing
 * [Requirement].
 *
 * When verification type is document, [Requirement.BIOMETRICCONSENT] must be present, transition
 * to [ConsentDestination].
 *
 * When verification type is not document, [Requirement.BIOMETRICCONSENT] is not present,
 * transition to [IndividualDestination].
 */
@Composable
internal fun InitialLoadingScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    fallbackUrlLauncher: FallbackUrlLauncher
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current
    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
            navController.navigateToErrorScreenWithDefaultValues(context)
        }
    ) {
        LaunchedEffect(Unit) {
            // Navigate to WebView screen instead of launching Chrome Custom Tab
            navController.navigateTo(VerificationWebViewDestination)
        }
    }
}
