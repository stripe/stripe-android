package com.stripe.android.financialconnections.features.partnerauth

import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.stripe.android.financialconnections.domain.prepane.Body
import com.stripe.android.financialconnections.domain.prepane.Cta
import com.stripe.android.financialconnections.domain.prepane.Display
import com.stripe.android.financialconnections.domain.prepane.OauthPrepane
import com.stripe.android.financialconnections.domain.prepane.PartnerNotice
import com.stripe.android.financialconnections.domain.prepane.Text
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.features.common.InstitutionPlaceholder
import com.stripe.android.financialconnections.features.common.InstitutionPlannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnknownErrorContent
import com.stripe.android.financialconnections.features.common.InstitutionUnplannedDowntimeErrorContent
import com.stripe.android.financialconnections.features.common.LoadingContent
import com.stripe.android.financialconnections.features.common.PartnerCallout
import com.stripe.android.financialconnections.features.common.UnclassifiedErrorContent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.PartnerAuthViewEffect.OpenPartnerAuth
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

@Composable
internal fun PartnerAuthScreen() {
    // activity view model
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val parentViewModel = parentViewModel()
    val webAuthFlow = activityViewModel.collectAsState { it.webAuthFlow }

    // step view model
    val viewModel: PartnerAuthViewModel = mavericksViewModel()
    val state: State<PartnerAuthState> = viewModel.collectAsState()

    ObserveViewEffect(state, activityViewModel, viewModel)
    LaunchedEffect(webAuthFlow.value) {
        viewModel.onWebAuthFlowFinished(webAuthFlow.value)
    }
    PartnerAuthScreenContent(
        state = state.value,
        onContinueClick = viewModel::onLaunchAuthClick,
        onSelectAnotherBank = viewModel::onSelectAnotherBank,
        onEnterDetailsManually = viewModel::onEnterDetailsManuallyClick,
        onCloseClick = { parentViewModel.onCloseNoConfirmationClick(Pane.PARTNER_AUTH) },
        onCloseFromErrorClick = parentViewModel::onCloseFromErrorClick
    )
}

@Composable
private fun ObserveViewEffect(
    state: State<PartnerAuthState>,
    activityViewModel: FinancialConnectionsSheetNativeViewModel,
    viewModel: PartnerAuthViewModel
) {
    LaunchedEffect(state.value.viewEffect) {
        when (val viewEffect = state.value.viewEffect) {
            null -> Unit
            is OpenPartnerAuth -> {
                activityViewModel.openPartnerAuthFlowInBrowser(viewEffect.url)
                viewModel.onViewEffectLaunched()
            }
        }
    }
}

@Composable
private fun PartnerAuthScreenContent(
    state: PartnerAuthState,
    onContinueClick: () -> Unit,
    onSelectAnotherBank: () -> Unit,
    onEnterDetailsManually: () -> Unit,
    onCloseClick: () -> Unit,
    onCloseFromErrorClick: (Throwable) -> Unit
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
                stringResource(id = R.string.stripe_partnerauth_loading_title),
                stringResource(id = R.string.stripe_partnerauth_loading_desc)
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
                onContinueClick = onContinueClick,
                onSelectAnotherBank = onSelectAnotherBank
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
    onSelectAnotherBank: () -> Unit
) {
    when (authenticationStatus) {
        is Uninitialized -> when (payload.authSession.isOAuth) {
            true -> if (payload.authSession.display == null) DefaultPrePaneContent(
                institution = payload.institution,
                flow = payload.authSession.flow,
                isStripeDirect = payload.isStripeDirect,
                showPartnerDisclosure = payload.authSession.showPartnerDisclosure ?: false,
                onContinueClick = onContinueClick
            ) else InstitutionalPrePaneContent(
                isStripeDirect = payload.isStripeDirect,
                onContinueClick = onContinueClick,
                content = payload.authSession.display.text.oauthPrepane
            )

            false -> LoadingContent(
                stringResource(id = R.string.stripe_partnerauth_loading_title),
                stringResource(id = R.string.stripe_partnerauth_loading_desc)
            )
        }

        is Loading, is Success -> LoadingContent(
            stringResource(id = R.string.stripe_partnerauth_loading_title),
            stringResource(id = R.string.stripe_partnerauth_loading_desc)
        )

        is Fail -> {
            // TODO@carlosmuvi translate error type to specific error screen.
            InstitutionUnknownErrorContent(onSelectAnotherBank)
        }
    }
}

@Composable
private fun DefaultPrePaneContent(
    institution: FinancialConnectionsInstitution,
    flow: Flow?,
    showPartnerDisclosure: Boolean,
    isStripeDirect: Boolean,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(
                top = 8.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = 24.dp
            )
    ) {
        val modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
        StripeImage(
            url = institution.icon?.default ?: "",
            contentDescription = null,
            imageLoader = LocalImageLoader.current,
            errorContent = { InstitutionPlaceholder(modifier) },
            modifier = modifier
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.stripe_prepane_title, institution.name),
            style = FinancialConnectionsTheme.typography.subtitle
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.stripe_prepane_desc, institution.name),
            style = FinancialConnectionsTheme.typography.body
        )
        Spacer(modifier = Modifier.weight(1f))
        if (flow != null && showPartnerDisclosure) PartnerCallout(flow, isStripeDirect)
        Spacer(modifier = Modifier.size(16.dp))
        FinancialConnectionsButton(
            onClick = onContinueClick,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.stripe_prepane_continue),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InstitutionalPrePaneContent(
    isStripeDirect: Boolean,
    onContinueClick: () -> Unit,
    content: OauthPrepane
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
        AnnotatedText(
            text = title,
            onClickableTextClick = { },
            defaultStyle = FinancialConnectionsTheme.typography.subtitle,
            annotationStyles = mapOf(
                StringAnnotation.BOLD to FinancialConnectionsTheme.typography.subtitleEmphasized.toSpanStyle()
            )
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            // CONTENT
            content.body.forEach { bodyItem ->
                when (bodyItem) {
                    is Body.Image -> {
                        GifWebView(bodyItem.content.default!!)
                    }
                    is Body.Text -> AnnotatedText(
                        text = TextResource.Text(fromHtml(bodyItem.content)),
                        onClickableTextClick = { },
                        defaultStyle = FinancialConnectionsTheme.typography.body,
                        annotationStyles = mapOf(
                            StringAnnotation.BOLD to FinancialConnectionsTheme.typography.bodyEmphasized.toSpanStyle()
                        )
                    )
                }
            }

            PartnerCallout(
                isStripeDirect = isStripeDirect,
                content.partnerNotice
            )
        }
        Box {
            FinancialConnectionsButton(
                onClick = onContinueClick,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.stripe_prepane_continue),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GifWebView(gifUrl: String) {
    val state = rememberWebViewStateWithHTMLData(
        "<html><body><img style=\"width: 100%\" src=\"$gifUrl\"></body></html>"
    )
    WebView(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp),
        onCreated = { it: WebView ->
            it.isVerticalScrollBarEnabled = false
            it.isVerticalFadingEdgeEnabled = false
        },
        state = state
    )
}

@Composable
@Preview
internal fun InstitutionalPrepaneContentPreview() {
    FinancialConnectionsPreview {
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
                                        title = "Sign in with **Banco del Nabo**",
                                        body = listOf(
                                            Body.Text(
                                                "Some very large text will most likely go here!"
                                            ),
                                            Body.Image(
                                                Image(
                                                    "https://media.tenor.com/H04kLkyt_tUAAAAM/dog-little-dog.gif"
                                                )
                                            ),
                                            Body.Text(
                                                "Some very large text will most likely go here!"
                                            ),
                                        ),
                                        cta = Cta(
                                            icon = null,
                                            text = "Continue!"
                                        ),
                                        institutionIcon = Image("https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--reserve-primary-3x.png"),
                                        partnerNotice = PartnerNotice(
                                            partnerIcon = Image("https://b.stripecdn.com/connections-statics-srv/assets/SailIcon--reserve-primary-3x.png"),
                                            text = "LOLOLOLOLOLOLOLOLOL"
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
            onCloseClick = {}
        ) {}
    }
}
