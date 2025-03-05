package com.stripe.android.identity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.stripe.android.camera.CameraPermissionCheckingActivity
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.identity.IdentityVerificationSheet.VerificationFlowResult
import com.stripe.android.identity.injection.DaggerIdentityActivityFallbackComponent
import com.stripe.android.identity.injection.IdentityActivitySubcomponent
import com.stripe.android.identity.ui.VerificationWebViewScreen
import com.stripe.android.identity.viewmodel.IdentityViewModel
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import android.util.Log

/**
 * Host activity to perform Identity verification.
 */
internal class IdentityActivity :
    CameraPermissionCheckingActivity(),
    VerificationFlowFinishable,
    Injectable<Context> {
    @VisibleForTesting
    internal lateinit var navController: NavController

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory =
        IdentityViewModel.IdentityViewModelFactory(
            { application },
            { subcomponent }
        )

    private val starterArgs: IdentityVerificationSheetContract.Args by lazy {
        requireNotNull(IdentityVerificationSheetContract.Args.fromIntent(intent)) {
            EMPTY_ARG_ERROR
        }
    }

    private val identityViewModel: IdentityViewModel by viewModels { viewModelFactory }

    private lateinit var subcomponent: IdentityActivitySubcomponent

    @Inject
    lateinit var subComponentBuilderProvider: Provider<IdentityActivitySubcomponent.Builder>

    @Inject
    @UIContext
    lateinit var uiContext: CoroutineContext

    @Inject
    @IOContext
    lateinit var workContext: CoroutineContext

    override fun fallbackInitialize(arg: Context): Injector? {
        DaggerIdentityActivityFallbackComponent.builder()
            .context(arg)
            .build().inject(this)
        return null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_PRESENTED, true)
    }

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("IdentitySDK", "IdentityActivity onCreate")
        injectWithFallback(
            starterArgs.injectorKey,
            this.applicationContext
        )
        subcomponent = subComponentBuilderProvider.get()
            .args(starterArgs)
            .cameraPermissionEnsureable(this)
            .appSettingsOpenable(this)
            .verificationFlowFinishable(this)
            .identityViewModelFactory(viewModelFactory)
            .build()
        identityViewModel.retrieveAndBufferVerificationPage()

        identityViewModel.observeForVerificationPage(
            this,
            onSuccess = {
                if (savedInstanceState?.getBoolean(KEY_PRESENTED, false) != true) {
                    identityViewModel.identityAnalyticsRequestFactory.sheetPresented()
                }
            },
            onFailure = {
                identityViewModel.errorCause.postValue(it)
                finishWithResult(VerificationFlowResult.Failed(it))
            }
        )

        // Hide default top bar
        supportActionBar?.hide()

        setContent {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(text = applicationInfo.loadLabel(packageManager).toString())
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                finishWithResult(VerificationFlowResult.Canceled)
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.stripe_close),
                                    contentDescription = stringResource(id = R.string.stripe_description_close)
                                )
                            }
                        },
                        windowInsets = WindowInsets.statusBars
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    VerificationWebViewScreen(
                        identityViewModel = identityViewModel,
                        verificationFlowFinishable = this@IdentityActivity
                    )
                }
            }
        }
    }

    override fun finishWithResult(result: VerificationFlowResult) {
        Log.d("IdentitySDK", "IdentityActivity finishWithResult called with result: $result")
        identityViewModel.identityAnalyticsRequestFactory.sheetClosed(
            result.toString()
        )
        setResult(
            Activity.RESULT_OK,
            Intent().putExtras(result.toBundle())
        )
        Log.d("IdentitySDK", "IdentityActivity finishing...")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("IdentitySDK", "IdentityActivity onDestroy")
    }

    /**
     * Display the permission rational dialog without writing PERMISSION_RATIONALE_SHOWN, this would
     * prevent [showPermissionDeniedDialog] from being called and always trigger
     * [CameraPermissionCheckingActivity.requestCameraPermission].
     */
    override fun showPermissionRationaleDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(R.string.stripe_camera_permission_rationale)
            .setPositiveButton(R.string.stripe_ok) { _, _ ->
                requestCameraPermission()
            }
        builder.show()
    }

    // This should have neve been invoked as PERMISSION_RATIONALE_SHOWN is never written.
    // Identity has its own CameraPermissionDeniedFragment to handle this case.
    override fun showPermissionDeniedDialog() {
        // no-op
    }

    private companion object {
        const val EMPTY_ARG_ERROR =
            "IdentityActivity was started without arguments"

        const val KEY_PRESENTED = "presented"
    }
}
