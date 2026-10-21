package com.stripe.android.identity.networked

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_NETWORKED_IDENTITY_SAVE
import com.stripe.android.identity.navigation.NetworkedIdentitySaveDestination
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.ui.ScreenTransitionLaunchedEffect
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.launch
import com.stripe.android.paymentsheet.R as PaymentSheetR

/**
 * Offers to save this verification's ID to Link once nothing else is missing, before it's submitted: a submitted
 * verification can't record the save.
 */
@Composable
internal fun NetworkedIdentitySaveScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    networkedIdentityViewModel: NetworkedIdentityViewModel,
) {
    ScreenTransitionLaunchedEffect(
        identityViewModel = identityViewModel,
        screenName = SCREEN_NAME_NETWORKED_IDENTITY_SAVE,
    )
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    val attemptState by networkedIdentityViewModel.state.collectAsState()
    val entry by networkedIdentityViewModel.entry.collectAsState()
    val submit by identityViewModel.verificationPageSubmit.collectAsState()
    // An attempt that's starting may not show its sheet yet; finishing now would submit under the save. Done also
    // submits, from outside this screen.
    val enabled = !isSubmitting && !attemptState.isInProgress && submit.status != Status.LOADING
    val colors = networkedIdentityColors()
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 48.dp, end = 24.dp),
        ) {
            SaveOffer(networkedIdentityViewModel.includesSelfie, colors)
        }
        // Material pads each 40dp button to a 48dp touch target, which adds to the design's 16dp gap and 8dp margin.
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinkSaveButton(enabled = enabled, colors = colors, onSave = networkedIdentityViewModel::startSave)
            // Once the save is recorded, finishing would still save the ID.
            if (entry.offersSave) {
                FinishWithoutSavingButton(enabled = enabled, colors = colors) {
                    isSubmitting = true
                    coroutineScope.launch {
                        networkedIdentityViewModel.declineSave()
                        identityViewModel.submitAndNavigate(navController, NetworkedIdentitySaveDestination.ROUTE.route)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishWithoutSavingButton(enabled: Boolean, colors: NetworkedIdentityColors, onFinish: () -> Unit) {
    OutlinedButton(
        onClick = onFinish,
        enabled = enabled,
        shape = SaveButtonShape,
        border = BorderStroke(1.dp, colors.secondaryButtonBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent,
            contentColor = colors.secondaryButtonText,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SaveButtonHeight)
            .testTag(NI_SAVE_FINISH_TAG),
    ) {
        Text(
            text = stringResource(R.string.stripe_identity_link_finish_without_saving),
            style = NetworkedIdentityBodyStyle,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SaveOffer(includesSelfie: Boolean, colors: NetworkedIdentityColors) {
    Text(
        text = stringResource(R.string.stripe_identity_link_save_title),
        color = colors.saveText,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NI_SAVE_TITLE_TAG)
            .semantics { heading() },
    )
    Text(
        text = stringResource(
            if (includesSelfie) {
                R.string.stripe_identity_link_save_body_id_and_selfie
            } else {
                R.string.stripe_identity_link_save_body_id
            }
        ),
        color = colors.saveText,
        style = NetworkedIdentityBodyStyle,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag(NI_SAVE_BODY_TAG),
    )
    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SaveBullet(
            icon = R.drawable.stripe_identity_link_id,
            text = stringResource(
                if (includesSelfie) {
                    R.string.stripe_identity_link_save_reuse_id_and_selfie
                } else {
                    R.string.stripe_identity_link_save_reuse_id
                }
            ),
            colors = colors,
        )
        SaveBullet(
            icon = R.drawable.stripe_identity_link_lock,
            text = stringResource(R.string.stripe_identity_link_save_encrypted),
            colors = colors,
        )
    }
}

@Composable
private fun SaveBullet(@DrawableRes icon: Int, text: String, colors: NetworkedIdentityColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.linkPrimary,
            modifier = Modifier
                .background(colors.linkSurfaceSecondary, CircleShape)
                .padding(12.dp)
                .size(20.dp),
        )
        Text(text = text, color = colors.saveBulletText, style = NetworkedIdentityBodyStyle)
    }
}

/** "Save with [Link logo]": the button's background is the opposite of the screen's, so the logo is too. */
@Composable
private fun LinkSaveButton(enabled: Boolean, colors: NetworkedIdentityColors, onSave: () -> Unit) {
    val saveLabel = stringResource(R.string.stripe_identity_link_save_button)
    Button(
        onClick = onSave,
        enabled = enabled,
        shape = SaveButtonShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colors.linkPrimary,
            contentColor = colors.onLinkPrimary,
        ),
        elevation = null,
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SaveButtonHeight)
            .semantics { contentDescription = saveLabel }
            .testTag(NI_SAVE_BUTTON_TAG),
    ) {
        Text(
            text = stringResource(R.string.stripe_identity_link_save_with),
            style = NetworkedIdentityBodyStyle,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Image(
            painter = painterResource(
                if (MaterialTheme.colors.isLight) {
                    R.drawable.stripe_identity_link_logo_on_dark
                } else {
                    PaymentSheetR.drawable.stripe_link_logo_light
                }
            ),
            contentDescription = null,
            // Dims like the label: the logo keeps its own colors.
            alpha = if (enabled) 1f else ContentAlpha.disabled,
            modifier = Modifier.size(width = 53.dp, height = 18.dp),
        )
    }
}

private val SaveButtonShape = RoundedCornerShape(6.dp)
private val SaveButtonHeight = 40.dp

internal const val NI_SAVE_TITLE_TAG = "NetworkedIdentitySaveTitle"
internal const val NI_SAVE_BODY_TAG = "NetworkedIdentitySaveBody"
internal const val NI_SAVE_BUTTON_TAG = "NetworkedIdentitySaveButton"
internal const val NI_SAVE_FINISH_TAG = "NetworkedIdentitySaveFinish"
