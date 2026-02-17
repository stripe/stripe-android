package com.stripe.android.common.taptoadd.ui

import javax.inject.Inject

internal class InitialTapToAddScreenFactory @Inject constructor(
    private val collectingInteractorFactory: TapToAddCollectingInteractor.Factory,
) {
    fun createInitialScreen(): TapToAddNavigator.Screen {
        return TapToAddNavigator.Screen.Collecting(
            interactor = collectingInteractorFactory.create()
        )
    }
}
