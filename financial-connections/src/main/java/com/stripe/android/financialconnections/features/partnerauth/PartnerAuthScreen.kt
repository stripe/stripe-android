@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.partnerauth

import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewStateWithHTMLData
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.domain.Body
import com.stripe.android.financialconnections.domain.Cta
import com.stripe.android.financialconnections.domain.Display
import com.stripe.android.financialconnections.domain.Entry
import com.stripe.android.financialconnections.domain.OauthPrepane
import com.stripe.android.financialconnections.domain.PartnerNotice
import com.stripe.android.financialconnections.domain.Text
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.features.common.DataAccessBottomSheetContent
import com.stripe.android.financialconnections.features.common.InstitutionPlaceholder
import com.stripe.android.financialconnections.features.common.InstitutionPlannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnknownErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnplannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.PartnerCallout
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession.Flow
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.Image
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.presentation.parentViewModel
import com.stripe.android.financialconnections.ui.FinancialConnectionsPreview
import com.stripe.android.financialconnections.ui.LocalImageLoader
import com.stripe.android.financialconnections.ui.TextResource
import com.stripe.android.financialconnections.ui.components.AnnotatedText
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsButton
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsScaffold
import com.stripe.android.financialconnections.ui.components.FinancialConnectionsTopAppBar
import com.stripe.android.financialconnections.ui.components.StringAnnotation
import com.stripe.android.financialconnections.ui.sdui.fromHtml
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.uicore.image.StripeImage
import kotlinx.coroutines.launch

@Composable
internal fun PartnerAuthScreen() {
    // activity view model
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val parentViewModel = parentViewModel()
    val webAuthFlow = activityViewModel.collectAsState { it.webAuthFlow }
    val uriHandler = LocalUriHandler.current

    // step view model
    val viewModel: PartnerAuthViewModel = mavericksViewModel()

    val state: State<PartnerAuthState> = viewModel.collectAsState()

    val scope = rememberCoroutineScope()

    val bottomSheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    state.value.viewEffect?.let { viewEffect ->
        LaunchedEffect(viewEffect) {
            when (viewEffect) {
                is OpenBottomSheet -> bottomSheetState.show()
                is OpenUrl -> uriHandler.openUri(viewEffect.url)
                is OpenPartnerAuth -> {
                    activityViewModel.openPartnerAuthFlowInBrowser(viewEffect.url)
                    viewModel.onViewEffectLaunched()
                }
            }
        }
    }

    LaunchedEffect(webAuthFlow.value) {
        viewModel.onWebAuthFlowFinished(webAuthFlow.value)
    }

    PartnerAuthScreenContent(
        state = state.value,
        onContinueClick = viewModel::onLaunchAuthClick,
        onSelectAnotherBank = viewModel::onSelectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManuallyClick,
        modalBottomSheetState = bottomSheetState,
        onClickableTextClick = viewModel::onClickableTextClick,
        onConfirmModalClick = { scope.launch { bottomSheetState.hide() } },
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.PARTNER_AUTH) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun PartnerAuthScreenContent(
    state: PartnerAuthState,
    modalBottomSheetState: ModalBottomSheetState,
    onContinueClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onClickableTextClick: (String) -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onConfirmModalClick: () -> Unit,
) {
    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        sheetBackgroundColor = FinancialConnectionsTheme.colors.backgroundSurface,
        sheetShape = RoundedCornerShape(8.dp),
        scrimColor = FinancialConnectionsTheme.colors.textSecondary.copy(alpha = 0.5f),
        sheetContent = {
            state.dataAccess?.let {
                DataAccessBottomSheetContent(
                    dataDialog = it,
                    onConfirmModalClick = onConfirmModalClick,
                    onClickableTextClick = onClickableTextClick
                )
            } ?: Spacer(modifier = Modifier.size(16.dp))
        },
        content = {
            PartnerAuthScreenMainContent(
                state = state,
                onCloseClick = onCloseClick,
                onSelectAnotherBank = onSelectAnotherBank,
                onEnterDetailsManually = onEnterDetailsManually,
                onCloseFromErrorClick = onCloseFromErrorClick,
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick,
            )
        }
    )
}

@Composable
private fun PartnerAuthScreenMainContent(
    state: PartnerAuthState,
    onCloseClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit,
    onContinueClick: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    FinancialConnectionsScaffold(
        topBar = {
            FinancialConnectionsTopAppBar(
                showBack = state.canNavigateBack,
                onCloseClick = onCloseClick
            )
        }
    ) {
        when (val payload = state.payload) {
            Uninitialized, is Loading -> LoadingContent(
                title = stringResource(id = R.string.stripe_partnerauth_loading_title),
                content = stringResource(id = R.string.stripe_partnerauth_loading_desc)
            )

            is Fail -> ErrorContent(
                error = payload.error,
                onSelectAnotherBank = onSelectAnotherBank,
                onEnterDetailsManually = onEnterDetailsManually,
                onCloseFromErrorClick = onCloseFromErrorClick
            )

            is Success -> LoadedContent(
                authenticationStatus = state.authenticationStatus,
                payload = payload(),
                onClickableTextClick = onClickableTextClick,
                onContinueClick = onContinueClick,
                onSelectAnotherBank = onSelectAnotherBank,
            )
        }
    }
}

@Composable
fun ErrorContent(
    error: Throwable,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
) {
    when (error) {
        is InstitutionPlannedDowntimeError -> InstitutionPlannedDowntimeErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        is InstitutionUnplannedDowntimeError -> InstitutionUnplannedDowntimeErrorContent(
            exception = error,
            onSelectAnotherBank = onSelectAnotherBank,
            onEnterDetailsManually = onEnterDetailsManually
        )

        else -> UnclassifiedErrorContent(error, onCloseFromErrorClick)
    }
}

@Composable
private fun LoadedContent(
    authenticationStatus: Async<String>,
    payload: PartnerAuthState.Payload,
    onContinueClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onClickableTextClick: (String) -> Unit
) {
    when (authenticationStatus) {
        is Uninitialized -> when (payload.authSession.isOAuth) {
            true -> InstitutionalPrePaneContent(
                onContinueClick = onContinueClick,
                content = payload.authSession.display!!.text.oauthPrepane,
                onClickableTextClick = onClickableTextClick,
            )

            false -> LoadingContent(
                title = stringResource(id = R.string.stripe_partnerauth_loading_title),
                content = stringResource(id = R.string.stripe_partnerauth_loading_desc)
            )
        }

        is Loading -> BrowserLoadingContent()

        is Success -> LoadingContent(
            title = stringResource(R.string.stripe_account_picker_loading_title),
            content = stringResource(R.string.stripe_account_picker_loading_desc)
        )

        is Fail -> {
            // TODO@carlosmuvi translate error type to specific error screen.
            InstitutionUnknownErrorContent(onSelectAnotherBank)
        }
    }
}

@Composable
private fun BrowserLoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = FinancialConnectionsTheme.colors.iconBrand
        )
    }
}

@Composable
private fun InstitutionalPrePaneContent(
    onContinueClick: () -> Unit,
    content: OauthPrepane,
    onClickableTextClick: (String) -> Unit
) {
    val title = remember(content.title) {
        TextResource.Text(fromHtml(content.title))
    }
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                top = 16.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            )
    ) {
        content.institutionIcon?.default?.let {
            val institutionIconModifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(6.dp))
            StripeImage(
                url = it,
                contentDescription = null,
                imageLoader = LocalImageLoader.current,
                errorContent = { InstitutionPlaceholder(institutionIconModifier) },
                modifier = institutionIconModifier
            )
            Spacer(modifier = Modifier.size(16.dp))
        }
        AnnotatedText(
            text = title,
            onClickableTextClick = { },
            defaultStyle = FinancialConnectionsTheme.typography.subtitle,
            annotationStyles = mapOf(
                StringAnnotation.BOLD to FinancialConnectionsTheme.typography.subtitleEmphasized.toSpanStyle()
            )
        )
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // CONTENT
            content.body.entries.forEachIndexed { index, bodyItem ->
                when (bodyItem) {
                    is Entry.Image -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    shape = RoundedCornerShape(8.dp),
                                    color = FinancialConnectionsTheme.colors.backgroundContainer
                                )

                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.stripe_prepane_phone_bg),
                                contentDescription = "Test",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(PHONE_BACKGROUND_HEIGHT_DP.dp)
                                    .height(PHONE_BACKGROUND_WIDTH_DP.dp)
                            )
                            GifWebView(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(PHONE_BACKGROUND_HEIGHT_DP.dp)
                                    .height(PHONE_BACKGROUND_WIDTH_DP.dp)
                                    .padding(horizontal = 16.dp),
                                bodyItem.content.default!!
                            )
                        }
                    }

                    is Entry.Text -> AnnotatedText(
                        text = TextResource.Text(fromHtml(bodyItem.content)),
                        onClickableTextClick = onClickableTextClick,
                        defaultStyle = FinancialConnectionsTheme.typography.body,
                        annotationStyles = mapOf(
                            StringAnnotation.BOLD to FinancialConnectionsTheme.typography.bodyEmphasized.toSpanStyle(),
                            StringAnnotation.CLICKABLE to FinancialConnectionsTheme.typography.bodyEmphasized
                                .toSpanStyle()
                                .copy(color = FinancialConnectionsTheme.colors.textBrand)
                        )
                    )
                }
                if (index != content.body.entries.lastIndex) {
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
            Box(modifier = Modifier.weight(1f))
            content.partnerNotice?.let {
                Spacer(modifier = Modifier.size(16.dp))
                PartnerCallout(
                    partnerNotice = content.partnerNotice,
                    onClickableTextClick = onClickableTextClick
                )
            }
        }
        FinancialConnectionsButton(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = content.cta.text,
                    textAlign = TextAlign.Center
                )
                content.cta.icon?.default?.let {
                    Spacer(modifier = Modifier.size(12.dp))
                    StripeImage(
                        url = it,
                        contentDescription = null,
                        imageLoader = LocalImageLoader.current,
                        errorContent = { },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GifWebView(
    modifier: Modifier,
    gifUrl: String
) {
    val state = rememberWebViewStateWithHTMLData(
        "<html><body><img style=\"width: 100%\" src=\"$gifUrl\"></body></html>"
    )
    WebView(
        modifier = modifier,
        onCreated = { it: WebView ->
            it.isVerticalScrollBarEnabled = false
            it.isVerticalFadingEdgeEnabled = false
        },
        state = state
    )
}

@Preview(
    group = "Partner Auth",
    name = "Institutional prepane"
)
@Composable
internal fun InstitutionalPrepaneContentPreview() {
    FinancialConnectionsPreview {
        val sampleImage =
            "https://b.stripecdn.com/connections-statics-srv/assets/PrepaneAsset--account_numbers-capitalone-2x.gif"
        PartnerAuthScreenContent(
            state = PartnerAuthState(
                payload = Success(
                    PartnerAuthState.Payload(
                        institution = FinancialConnectionsInstitution(
                            id = "id",
                            name = "name",
                            url = "url",
                            featured = true,
                            icon = null,
                            logo = null,
                            featuredOrder = null,
                            mobileHandoffCapable = false
                        ),
                        authSession = FinancialConnectionsAuthorizationSession(
                            flow = Flow.FINICITY_CONNECT_V2_OAUTH,
                            showPartnerDisclosure = true,
                            _isOAuth = true,
                            nextPane = Pane.PARTNER_AUTH,
                            id = "1234",
                            display = Display(
                                Text(
                                    oauthPrepane = OauthPrepane(
                                        title = "Sign in with Sample bank",
                                        body = Body(
                                            listOf(
                                                Entry.Text(
                                                    "Some very large text will most likely go here!" +
                                                        "Some very large text will most likely go here!"
                                                ),
                                                Entry.Image(
                                                    Image(sampleImage)
                                                ),
                                                Entry.Text(
                                                    "Some very large text will most likely go here!"
                                                ),
                                                Entry.Text(
                                                    "Some very large text will most likely go here!"
                                                ),
                                                Entry.Text(
                                                    "Some very large text will most likely go here!"
                                                )
                                            )
                                        ),
                                        cta = Cta(
                                            icon = null,
                                            text = "Continue!"
                                        ),
                                        institutionIcon = Image(sampleImage),
                                        partnerNotice = PartnerNotice(
                                            partnerIcon = Image(sampleImage),
                                            text = "Stripe works with partners like MX to reliably" +
                                                " offer access to thousands of financial institutions." +
                                                " Learn more"
                                        )
                                    )
                                )
                            )
                        ),
                        isStripeDirect = false
                    )
                ),
                authenticationStatus = Uninitialized,
                viewEffect = null
            ),
            onContinueClick = {},
            onSelectAnotherBank = {},
            onEnterDetailsManually = {},
            onCloseClick = {},
            modalBottomSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden
            ),
            onClickableTextClick = {},
            onCloseFromErrorClick = {},
            onConfirmModalClick = {}
        )
    }
}

private const val PHONE_BACKGROUND_WIDTH_DP = 272
private const val PHONE_BACKGROUND_HEIGHT_DP = 264
