package com.stripe.android.identity.viewmodel

import androidx.compose.material.ExperimentalMaterialApi
import androidx.lifecycle.ViewModel
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel to control if a bottomsheet should show.
 */
@ExperimentalMaterialApi
internal class BottomSheetViewModel : ViewModel() {

    data class BottomSheetState(
        val shouldShow: Boolean,
        val content: VerificationPageStaticContentBottomSheetContent?
    )

    private val _bottomSheetState: MutableStateFlow<BottomSheetState> = MutableStateFlow(
        BottomSheetState(
            false,
            null
        )
    )
    val bottomSheetState: StateFlow<BottomSheetState> =
        _bottomSheetState

    fun showBottomSheet(
        bottomSheetContent: VerificationPageStaticContentBottomSheetContent
    ) {
        _bottomSheetState.update {
            BottomSheetState(shouldShow = true, content = bottomSheetContent)
        }
    }

    fun dismissBottomSheet() {
        _bottomSheetState.update { INITIAL_STATE }
    }

    companion object {
        val INITIAL_STATE = BottomSheetState(false, null)
    }
}
