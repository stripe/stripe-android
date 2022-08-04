package com.stripe.android.identity.example

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentColor
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.kittinunf.result.Result
import com.google.android.material.composethemeadapter.MdcTheme
import com.stripe.android.identity.IdentityVerificationSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class ComposeExampleActivity : ComponentActivity() {
    protected abstract val getBrandLogoResId: Int

    private val viewModel: IdentityExampleViewModel by viewModels()

    private val logoUri: Uri
        get() = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(getBrandLogoResId))
            .appendPath(resources.getResourceTypeName(getBrandLogoResId))
            .appendPath(resources.getResourceEntryName(getBrandLogoResId))
            .build()

    data class IdentitySubmissionState(
        val shouldUseNativeSdk: Boolean = true,
        val allowDrivingLicense: Boolean = true,
        val allowPassport: Boolean = true,
        val allowId: Boolean = true,
        val requireLiveCapture: Boolean = true,
        val requireId: Boolean = false,
        val requireSelfie: Boolean = false
    )

    sealed class LoadingState {
        object Idle : LoadingState()
        object Loading : LoadingState()
        class Result(val vsId: String = "", val resultString: String) : LoadingState()
    }

    @Composable
    fun ExampleScreen() {
        val scaffoldState = rememberScaffoldState()
        val coroutineScope = rememberCoroutineScope()
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar {
                    Text(
                        text = stringResource(id = R.string.compose_example),
                        modifier = Modifier.padding(start = 10.dp),
                        fontWeight = FontWeight.Bold,
                        color = LocalContentColor.current
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                val (submissionState, onSubmissionStateChanged) = remember {
                    mutableStateOf(IdentitySubmissionState())
                }
                NativeOrWeb(submissionState, onSubmissionStateChanged)
                Divider()
                AllowedDocumentTypes(submissionState, onSubmissionStateChanged)
                RequireDocTypes(submissionState, onSubmissionStateChanged)
                Divider()

                val (loadingState, onLoadingStateChanged) = remember {
                    mutableStateOf<LoadingState>(LoadingState.Idle)
                }
                var vsId by remember { mutableStateOf("") }
                val identityVerificationSheet = IdentityVerificationSheet.createForCompose(
                    configuration = IdentityVerificationSheet.Configuration(
                        // Or use webImage by
                        // brandLogo = Uri.parse("https://path/to/a/logo.jpg")
                        brandLogo = logoUri
                    )
                ) { result ->
                    when (result) {
                        is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
                            onLoadingStateChanged(
                                LoadingState.Result(
                                    vsId = vsId,
                                    resultString = "Verification result: ${result.javaClass.simpleName} - ${result.throwable}"
                                )
                            )
                        }
                        is IdentityVerificationSheet.VerificationFlowResult.Canceled -> {
                            onLoadingStateChanged(
                                LoadingState.Result(
                                    vsId = vsId,
                                    resultString = "Verification result: ${result.javaClass.simpleName}"
                                )
                            )
                        }
                        is IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                            onLoadingStateChanged(
                                LoadingState.Result(
                                    vsId = vsId,
                                    resultString = "Verification result: ${result.javaClass.simpleName}"
                                )
                            )
                        }
                    }
                }

                LoadVSView(
                    loadingState,
                    submissionState,
                    scaffoldState,
                    coroutineScope,
                    onLoadingStateChanged
                ) { verificationSessionId, ephemeralKeySecret, url ->
                    vsId = verificationSessionId
                    if (submissionState.shouldUseNativeSdk) {
                        identityVerificationSheet.present(verificationSessionId, ephemeralKeySecret)
                    } else {
                        onLoadingStateChanged(
                            LoadingState.Result(vsId, "web redirect")
                        )
                        CustomTabsIntent.Builder().build()
                            .launchUrl(this@ComposeExampleActivity, Uri.parse(url))
                    }
                }
            }
        }
    }

    @Composable
    fun NativeOrWeb(
        identitySubmissionState: IdentitySubmissionState,
        onSubmissionStateChangedListener: (IdentitySubmissionState) -> Unit
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = identitySubmissionState.shouldUseNativeSdk,
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            shouldUseNativeSdk = true
                        )
                    )
                }
            )
            ClickableText(
                text = AnnotatedString(getString(R.string.use_native)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            shouldUseNativeSdk = true
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.width(40.dp))

            RadioButton(
                selected = !identitySubmissionState.shouldUseNativeSdk,
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            shouldUseNativeSdk = false
                        )
                    )
                }
            )
            ClickableText(
                text = AnnotatedString(getString(R.string.use_web)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            shouldUseNativeSdk = false
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.width(40.dp))
        }
    }

    @Composable
    fun AllowedDocumentTypes(
        identitySubmissionState: IdentitySubmissionState,
        onSubmissionStateChangedListener: (IdentitySubmissionState) -> Unit
    ) {
        Text(
            text = stringResource(id = R.string.allowed_types),
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 10.dp, top = 16.dp)
        )
        Row(
            modifier = Modifier.padding(start = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = identitySubmissionState.allowDrivingLicense,
                onCheckedChange = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowDrivingLicense = it
                        )
                    )
                },
                modifier = Modifier.padding(end = 0.dp)
            )
            ClickableText(
                text = AnnotatedString(stringResource(id = R.string.driver_license)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowDrivingLicense = !identitySubmissionState.allowDrivingLicense
                        )
                    )
                }
            )

            Checkbox(checked = identitySubmissionState.allowPassport, onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        allowPassport = it
                    )
                )
            })
            ClickableText(
                text = AnnotatedString(stringResource(id = R.string.passport)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowPassport = !identitySubmissionState.allowPassport
                        )
                    )
                }
            )

            Checkbox(checked = identitySubmissionState.allowId, onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        allowId = it
                    )
                )
            })
            ClickableText(
                text = AnnotatedString(stringResource(id = R.string.id_card)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowId = !identitySubmissionState.allowId
                        )
                    )
                }
            )
        }
    }

    @Composable
    fun RequireDocTypes(
        identitySubmissionState: IdentitySubmissionState,
        onSubmissionStateChangedListener: (IdentitySubmissionState) -> Unit
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = identitySubmissionState.requireLiveCapture,
                onCheckedChange = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            requireLiveCapture = it
                        )
                    )
                }
            )
            ClickableText(
                text = AnnotatedString(stringResource(id = R.string.require_live_capture)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            requireLiveCapture = !identitySubmissionState.requireLiveCapture
                        )
                    )
                }
            )
        }

        Row(
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (identitySubmissionState.shouldUseNativeSdk) {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireId = false
                    )
                )
                Checkbox(
                    checked = identitySubmissionState.requireId,
                    onCheckedChange = {
                        // no-op
                    },
                    enabled = false
                )
                Text(
                    text = stringResource(id = R.string.require_id_number),
                    style = TextStyle.Default,
                    color = Color.Unspecified.copy(alpha = 0.5f)
                )
            } else {
                Checkbox(
                    checked = identitySubmissionState.requireId,
                    onCheckedChange = {
                        onSubmissionStateChangedListener(
                            identitySubmissionState.copy(
                                requireId = it
                            )
                        )
                    }
                )
                ClickableText(
                    text = AnnotatedString(stringResource(id = R.string.require_id_number)),
                    onClick = {
                        onSubmissionStateChangedListener(
                            identitySubmissionState.copy(
                                requireId = !identitySubmissionState.requireId
                            )
                        )
                    }
                )
            }
        }

        Row(
            modifier = Modifier.padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = identitySubmissionState.requireSelfie,
                onCheckedChange = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            requireSelfie = it
                        )
                    )
                }
            )
            ClickableText(
                text = AnnotatedString(stringResource(id = R.string.require_matching_selfie)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            requireSelfie = !identitySubmissionState.requireSelfie
                        )
                    )
                }
            )
        }
    }

    @Composable
    internal fun LoadVSView(
        loadingState: LoadingState,
        identitySubmissionState: IdentitySubmissionState,
        scaffoldState: ScaffoldState,
        coroutineScope: CoroutineScope,
        onLoadingStateChanged: (LoadingState) -> Unit,
        onPostResult: (String, String, String) -> Unit
    ) {
        when (loadingState) {
            LoadingState.Idle -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    LoadingButton(
                        enabled = true,
                        submissionState = identitySubmissionState,
                        scaffoldState = scaffoldState,
                        coroutineScope = coroutineScope,
                        onLoadingStateChanged = onLoadingStateChanged,
                        onPostResult = onPostResult
                    )
                }
            }
            LoadingState.Loading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                    }
                    LoadingButton(
                        enabled = false,
                        submissionState = identitySubmissionState,
                        scaffoldState = scaffoldState,
                        coroutineScope = coroutineScope,
                        onLoadingStateChanged = onLoadingStateChanged,
                        onPostResult = onPostResult
                    )
                }
            }
            is LoadingState.Result -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        SelectionContainer {
                            Text(
                                text = loadingState.vsId,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Text(
                            text = loadingState.resultString,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                    LoadingButton(
                        enabled = true,
                        submissionState = identitySubmissionState,
                        scaffoldState = scaffoldState,
                        coroutineScope = coroutineScope,
                        onLoadingStateChanged = onLoadingStateChanged,
                        onPostResult = onPostResult
                    )
                }
            }
        }
    }

    @Composable
    fun LoadingButton(
        enabled: Boolean,
        submissionState: IdentitySubmissionState,
        scaffoldState: ScaffoldState,
        coroutineScope: CoroutineScope,
        onLoadingStateChanged: (LoadingState) -> Unit,
        onPostResult: (String, String, String) -> Unit
    ) {
        Button(
            onClick = {
                coroutineScope.launch {
                    scaffoldState.snackbarHostState.showSnackbar("Getting verificationSessionId and ephemeralKeySecret from backend...")
                }
                onLoadingStateChanged(LoadingState.Loading)
                viewModel.postForResult(
                    allowDrivingLicense = submissionState.allowDrivingLicense,
                    allowPassport = submissionState.allowPassport,
                    allowId = submissionState.allowId,
                    requireLiveCapture = submissionState.requireLiveCapture,
                    requireId = submissionState.requireId,
                    requireSelfie = submissionState.requireSelfie
                ).observe(this) {
                    when (it) {
                        is Result.Success -> {
                            onPostResult(
                                it.value.verificationSessionId,
                                it.value.ephemeralKeySecret,
                                it.value.url
                            )
                        }
                        is Result.Failure -> {
                            onLoadingStateChanged(
                                LoadingState.Result(
                                    resultString = "Error generating verificationSessionId and ephemeralKeySecret: ${it.getException().message}"
                                )
                            )
                        }
                    }
                }
            },
            modifier = Modifier.padding(bottom = 40.dp),
            enabled = enabled
        ) {
            Text(text = stringResource(id = R.string.start_verification))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MdcTheme {
                ExampleScreen()
            }
        }
    }
}
