package com.stripe.android.identity.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.kittinunf.result.Result
import com.stripe.android.identity.IdentityVerificationSheet
import com.stripe.android.identity.example.BuildConfig
import com.stripe.android.identity.example.IdentityExampleViewModel
import com.stripe.android.identity.example.R
import com.stripe.android.identity.example.ui.IntegrationType.LINK
import com.stripe.android.identity.example.ui.IntegrationType.NATIVE
import com.stripe.android.identity.example.ui.IntegrationType.WEB
import kotlinx.coroutines.launch

internal enum class VerificationType(val value: String) {
    DOCUMENT("document"), ID_NUMBER("id_number"), ADDRESS("address"), PHONE("phone")
}

internal enum class IntegrationType {
    NATIVE, WEB, LINK
}

private data class IdentitySubmissionState(
    val integrationType: IntegrationType = NATIVE,
    val verificationType: VerificationType = VerificationType.DOCUMENT,
    val allowDrivingLicense: Boolean = true,
    val allowPassport: Boolean = true,
    val allowId: Boolean = true,
    val requireLiveCapture: Boolean = true,
    val requireId: Boolean = false,
    val requireSelfie: Boolean = false,
    val requireAddress: Boolean = false
)

private sealed class LoadingState {
    object Idle : LoadingState()
    object Loading : LoadingState()
    class Result(val highlight: String = "", val resultString: String = "") : LoadingState()
}

@Composable
internal fun ExampleScreen(
    configuration: IdentityVerificationSheet.Configuration,
    viewModel: IdentityExampleViewModel
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()
    val (submissionState, onSubmissionStateChanged) = remember {
        mutableStateOf(IdentitySubmissionState())
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar {
                Row {
                    Text(
                        text = stringResource(id = R.string.identity_example),
                        modifier = Modifier.padding(start = 10.dp),
                        fontWeight = FontWeight.Bold,
                        color = LocalContentColor.current
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "${BuildConfig.VERSION_CODE}(${BuildConfig.VERSION_NAME})")
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            IntegrationTypeUI(submissionState, onSubmissionStateChanged)
            Divider()
            TypeSelectUI(
                submissionState.verificationType,
                submissionState.integrationType
            ) { newVerificationType ->
                onSubmissionStateChanged(
                    submissionState.copy(
                        verificationType = newVerificationType
                    )
                )
            }
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .weight(weight = 1f, fill = true)
            ) {
                if (submissionState.verificationType == VerificationType.DOCUMENT) {
                    DocumentUI(submissionState, onSubmissionStateChanged)
                }
            }
            Divider()
            SubmitView(
                submissionState,
                scaffoldState,
                configuration,
                viewModel
            )
        }
    }
}

@Composable
private fun IntegrationTypeUI(
    identitySubmissionState: IdentitySubmissionState,
    onSubmissionStateChangedListener: (IdentitySubmissionState) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = identitySubmissionState.integrationType == NATIVE,
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        verificationType = VerificationType.DOCUMENT,
                        integrationType = NATIVE
                    )
                )
            }
        )
        StyledClickableText(
            text = AnnotatedString(stringResource(R.string.use_native)),
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        verificationType = VerificationType.DOCUMENT,
                        integrationType = NATIVE
                    )
                )
            }
        )
        Spacer(modifier = Modifier.width(40.dp))

        RadioButton(
            selected = identitySubmissionState.integrationType == WEB,
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        verificationType = VerificationType.DOCUMENT,
                        integrationType = WEB
                    )
                )
            }
        )
        StyledClickableText(
            text = AnnotatedString(stringResource(R.string.use_web)),
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        verificationType = VerificationType.DOCUMENT,
                        integrationType = WEB
                    )
                )
            }
        )
        Spacer(modifier = Modifier.width(40.dp))

        RadioButton(
            selected = identitySubmissionState.integrationType == LINK,
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        verificationType = VerificationType.DOCUMENT,
                        integrationType = LINK
                    )
                )
            }
        )
        StyledClickableText(
            text = AnnotatedString(stringResource(R.string.get_link)),
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        verificationType = VerificationType.DOCUMENT,
                        integrationType = LINK
                    )
                )
            }
        )
    }
}

@Composable
private fun TypeSelectUI(
    verificationType: VerificationType,
    integrationType: IntegrationType,
    onVerificationTypeChanged: (VerificationType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Text(
        text = stringResource(id = R.string.verification_type),
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 10.dp, top = 16.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
    ) {
        OutlinedTextField(
            value = verificationType.name,
            enabled = false,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable(onClick = { expanded = true }),
            placeholder = {
                Text(text = verificationType.name)
            },
            onValueChange = {}
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if (integrationType == NATIVE) {
                listOf(
                    VerificationType.DOCUMENT,
                    VerificationType.ID_NUMBER,
                    VerificationType.ADDRESS
                )
            } else {
                listOf(
                    VerificationType.DOCUMENT,
                    VerificationType.ID_NUMBER,
                    VerificationType.ADDRESS,
                    VerificationType.PHONE
                )
            }.forEach { type ->
                DropdownMenuItem(
                    onClick = {
                        onVerificationTypeChanged(type)
                        expanded = false
                    }
                ) {
                    Text(text = type.name)
                }
            }
        }
    }
}

@Composable
private fun DocumentUI(
    submissionState: IdentitySubmissionState,
    onSubmissionStateChanged: (IdentitySubmissionState) -> Unit
) {
    AllowedDocumentTypes(submissionState, onSubmissionStateChanged)
    RequireDocTypes(submissionState, onSubmissionStateChanged)
}

@Composable
private fun AllowedDocumentTypes(
    identitySubmissionState: IdentitySubmissionState,
    onSubmissionStateChangedListener: (IdentitySubmissionState) -> Unit
) {
    Text(
        text = stringResource(id = R.string.allowed_types),
        fontSize = 16.sp,
        modifier = Modifier.padding(start = 10.dp, top = 16.dp)
    )
    Column {
        Row(
            modifier = Modifier.padding(horizontal = 40.dp),
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

            StyledClickableText(
                text = AnnotatedString(stringResource(id = R.string.driver_license)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowDrivingLicense = !identitySubmissionState.allowDrivingLicense
                        )
                    )
                }
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = identitySubmissionState.allowPassport, onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        allowPassport = it
                    )
                )
            })
            StyledClickableText(
                text = AnnotatedString(stringResource(id = R.string.passport)),
                onClick = {
                    onSubmissionStateChangedListener(
                        identitySubmissionState.copy(
                            allowPassport = !identitySubmissionState.allowPassport
                        )
                    )
                }
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = identitySubmissionState.allowId, onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        allowId = it
                    )
                )
            })
            StyledClickableText(
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
}

@Composable
private fun RequireDocTypes(
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
        StyledClickableText(
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
        StyledClickableText(
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
        StyledClickableText(
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

    Row(
        modifier = Modifier.padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = identitySubmissionState.requireAddress,
            onCheckedChange = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireAddress = it
                    )
                )
            }
        )
        StyledClickableText(
            text = AnnotatedString(stringResource(id = R.string.require_address)),
            onClick = {
                onSubmissionStateChangedListener(
                    identitySubmissionState.copy(
                        requireAddress = !identitySubmissionState.requireAddress
                    )
                )
            }
        )
    }
}

@Composable
private fun LoadVSView(
    viewModel: IdentityExampleViewModel,
    loadingState: LoadingState,
    identitySubmissionState: IdentitySubmissionState,
    onLoadingStateChanged: (LoadingState) -> Unit,
    onPostResult: (String, String, String) -> Unit
) {
    when (loadingState) {
        LoadingState.Idle -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                LoadingButton(
                    viewModel = viewModel,
                    enabled = true,
                    submissionState = identitySubmissionState,
                    onLoadingStateChanged = onLoadingStateChanged,
                    onPostResult = onPostResult
                )
            }
        }
        LoadingState.Loading -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
                LoadingButton(
                    viewModel = viewModel,
                    enabled = false,
                    submissionState = identitySubmissionState,
                    onLoadingStateChanged = onLoadingStateChanged,
                    onPostResult = onPostResult
                )
            }
        }
        is LoadingState.Result -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    SelectionContainer {
                        Text(
                            text = loadingState.highlight,
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
                    viewModel = viewModel,
                    enabled = true,
                    submissionState = identitySubmissionState,
                    onLoadingStateChanged = onLoadingStateChanged,
                    onPostResult = onPostResult
                )
            }
        }
    }
}

@Composable
private fun LoadingButton(
    viewModel: IdentityExampleViewModel,
    enabled: Boolean,
    submissionState: IdentitySubmissionState,
    onLoadingStateChanged: (LoadingState) -> Unit,
    onPostResult: (String, String, String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    Button(
        onClick = {
            onLoadingStateChanged(LoadingState.Loading)
            viewModel.postForResult(
                type = submissionState.verificationType,
                allowDrivingLicense = submissionState.allowDrivingLicense,
                allowPassport = submissionState.allowPassport,
                allowId = submissionState.allowId,
                requireLiveCapture = submissionState.requireLiveCapture,
                requireId = submissionState.requireId,
                requireSelfie = submissionState.requireSelfie,
                requireAddress = submissionState.requireAddress
            ).observe(lifecycleOwner) {
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
        enabled = enabled
    ) {
        Text(text = stringResource(id = R.string.start_verification))
    }
}

@Composable
private fun SubmitView(
    submissionState: IdentitySubmissionState,
    scaffoldState: ScaffoldState,
    configuration: IdentityVerificationSheet.Configuration,
    viewModel: IdentityExampleViewModel,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val (loadingState, onLoadingStateChanged) = remember {
        mutableStateOf<LoadingState>(LoadingState.Idle)
    }
    var vsId by remember { mutableStateOf("") }

    val identityVerificationSheet =
        IdentityVerificationSheet.rememberIdentityVerificationSheet(
            configuration = configuration
        ) { result ->
            when (result) {
                is IdentityVerificationSheet.VerificationFlowResult.Failed -> {
                    onLoadingStateChanged(
                        LoadingState.Result(
                            highlight = vsId,
                            resultString = "Verification result: Failed - ${result.throwable}"
                        )
                    )
                }
                IdentityVerificationSheet.VerificationFlowResult.Canceled -> {
                    onLoadingStateChanged(
                        LoadingState.Result(
                            highlight = vsId,
                            resultString = "Verification result: Canceled"
                        )
                    )
                }
                IdentityVerificationSheet.VerificationFlowResult.Completed -> {
                    onLoadingStateChanged(
                        LoadingState.Result(
                            highlight = vsId,
                            resultString = "Verification result: Completed"
                        )
                    )
                }
            }
        }

    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = viewModel.vsData.value != null,
        onClick = {
            identityVerificationSheet.present(
                requireNotNull(viewModel.vsData.value),
                requireNotNull(viewModel.eakData.value)
            )
        }
    ) {
        Text(text = "Start previous verfication")
    }

    LoadVSView(
        viewModel,
        loadingState,
        submissionState,
        onLoadingStateChanged,
    ) { verificationSessionId, ephemeralKeySecret, url ->
        vsId = verificationSessionId
        when (submissionState.integrationType) {
            NATIVE -> {
                identityVerificationSheet.present(verificationSessionId, ephemeralKeySecret)
            }
            WEB -> {
                onLoadingStateChanged(
                    LoadingState.Result(vsId, "web redirect")
                )
                CustomTabsIntent.Builder().build()
                    .launchUrl(context, Uri.parse(url))
            }
            LINK -> {
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("verification link", url))
                coroutineScope.launch {
                    scaffoldState.snackbarHostState.showSnackbar("Link copied to clipboard!")
                }
                onLoadingStateChanged(
                    LoadingState.Result(url)
                )
            }
        }
    }
}

@Composable
private fun StyledClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    onClick: (Int) -> Unit
) {
    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        }
    }
    // NOTE(text-perf-review): It might be worthwhile writing a bespoke merge implementation that
    // will avoid reallocating if all of the options here are the defaults
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing
        )
    )

    ClickableText(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        onClick = onClick
    )
}
