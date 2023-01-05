package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.DocumentUploadSideInfo
import com.stripe.android.identity.ui.UploadMethod
import com.stripe.android.identity.ui.UploadScreen
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Fragment to upload front and back of a document.
 *
 */
internal abstract class IdentityUploadFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {

    @get:StringRes
    abstract val titleRes: Int

    @get:StringRes
    abstract val contextRes: Int

    @get:StringRes
    abstract val frontTextRes: Int

    @get:StringRes
    open var backTextRes: Int? = null

    @get:StringRes
    abstract val frontCheckMarkContentDescription: Int

    @get:StringRes
    open var backCheckMarkContentDescription: Int? = null

    abstract val destinationRoute: IdentityTopLevelDestination.DestinationRoute

    abstract val frontScanType: IdentityScanState.ScanType

    open var backScanType: IdentityScanState.ScanType? = null

    abstract val collectedDataParamType: CollectedDataParam.Type

    protected val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        identityViewModel.imageHandler.updateScanTypes(
            frontScanType,
            backScanType
        )
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            UploadScreen(
                navController = findNavController(),
                identityViewModel = identityViewModel,
                collectedDataParamType = collectedDataParamType,
                route = destinationRoute.route,
                title = stringResource(id = titleRes),
                context = stringResource(id = contextRes),
                frontInfo =
                DocumentUploadSideInfo(
                    stringResource(id = frontTextRes),
                    stringResource(id = frontCheckMarkContentDescription),
                    frontScanType
                ) { uploadMethod ->
                    when (uploadMethod) {
                        UploadMethod.TAKE_PHOTO -> {
                            identityViewModel.imageHandler.takePhotoFront(requireContext())
                        }
                        UploadMethod.CHOOSE_PHOTO -> {
                            identityViewModel.imageHandler.chooseImageFront()
                        }
                    }
                },
                backInfo =
                if (backTextRes != null && backCheckMarkContentDescription != null && backScanType != null) {
                    DocumentUploadSideInfo(
                        stringResource(id = backTextRes!!),
                        stringResource(id = backCheckMarkContentDescription!!),
                        backScanType!!
                    ) { uploadMethod ->
                        when (uploadMethod) {
                            UploadMethod.TAKE_PHOTO -> {
                                identityViewModel.imageHandler.takePhotoBack(requireContext())
                            }
                            UploadMethod.CHOOSE_PHOTO -> {
                                identityViewModel.imageHandler.chooseImageBack()
                            }
                        }
                    }
                } else {
                    null
                },
                shouldShowTakePhoto = requireNotNull(arguments).getBoolean(
                    ARG_SHOULD_SHOW_TAKE_PHOTO
                ),
                shouldShowChoosePhoto = requireNotNull(arguments).getBoolean(
                    ARG_SHOULD_SHOW_CHOOSE_PHOTO
                )
            )
        }
    }
}
