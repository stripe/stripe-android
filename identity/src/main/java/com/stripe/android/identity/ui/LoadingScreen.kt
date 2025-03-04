package com.stripe.android.identity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.viewmodel.IdentityViewModel

val LOADING_SCREEN_TAG = "Loading"

@Composable
internal fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTag = LOADING_SCREEN_TAG
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(bottom = 32.dp))
        Text(text = stringResource(id = R.string.stripe_loading), fontSize = 24.sp)
    }
}

/**
 * Ensures [IdentityViewModel.verificationPage] is valid and compose.
 * Otherwise navigate to default error.
 */
@Composable
internal fun CheckVerificationPageAndCompose(
    identityViewModel: IdentityViewModel,
    navController: NavController,
    onSuccess: @Composable (VerificationPage) -> Unit
) {
    val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
    val context = LocalContext.current
    CheckVerificationPageAndCompose(
        verificationPageResource = verificationPageState,
        onError = {
            identityViewModel.errorCause.postValue(it)
//            navController.navigateToErrorScreenWithDefaultValues(context)
        },
        onSuccess = onSuccess
    )
}

@Composable
internal fun CheckVerificationPageAndCompose(
    verificationPageResource: Resource<VerificationPage>,
    onError: (Throwable) -> Unit,
    onSuccess: @Composable (VerificationPage) -> Unit
) {
    when (verificationPageResource.status) {
        Status.SUCCESS -> {
            onSuccess(requireNotNull(verificationPageResource.data))
        }
        Status.ERROR -> {
            LaunchedEffect(Unit) {
                onError(
                    verificationPageResource.throwable
                        ?: IllegalStateException("Failed to get verificationPage")
                )
            }
        }
        Status.LOADING -> {
            LoadingScreen()
        }
        Status.IDLE -> {
            // no-op
        }
    }
}

//@Composable
//internal fun CheckVerificationPageModelFilesAndCompose(
//    identityViewModel: IdentityViewModel,
//    navController: NavController,
//    onSuccess: @Composable (IdentityViewModel.PageAndModelFiles) -> Unit
//) {
//    val verificationPageState by identityViewModel.pageAndModelFiles.observeAsState(Resource.loading())
//    val context = LocalContext.current
//    when (verificationPageState.status) {
//        Status.SUCCESS -> {
//            onSuccess(requireNotNull(verificationPageState.data))
//        }
//        Status.LOADING -> {
//            LoadingScreen()
//        } // no-op
//        Status.IDLE -> {} // no-op
//        Status.ERROR -> {
//            identityViewModel.errorCause.postValue(
//                InvalidResponseException(
//                    cause = verificationPageState.throwable,
//                    message = verificationPageState.message
//                )
//            )
//            navController.navigateToErrorScreenWithDefaultValues(context)
//        }
//    }
//}
