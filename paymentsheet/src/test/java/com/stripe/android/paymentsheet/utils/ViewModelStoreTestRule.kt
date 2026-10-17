package com.stripe.android.paymentsheet.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Clears the [ViewModel]s built during a test when the test finishes.
 *
 * ViewModels launch perpetual collectors on their `viewModelScope` (and on any injected
 * `customViewModelScope`) that only stop when the ViewModel is cleared. Unit tests construct
 * ViewModels directly rather than through a [androidx.lifecycle.ViewModelProvider], so nothing ever
 * clears them and those scopes leak across tests. Registering each ViewModel here and calling
 * [ViewModelStore.clear] on finish cancels `viewModelScope` and invokes `onCleared()`, matching what
 * the framework does when the owner is destroyed.
 *
 * Register ViewModels via [track].
 */
class ViewModelStoreTestRule : TestWatcher() {
    private val viewModelStore = ViewModelStore()
    private var trackedCount = 0

    fun <T : ViewModel> track(viewModel: T): T = viewModel.also {
        viewModelStore.put("tracked_${trackedCount++}", it)
    }

    override fun finished(description: Description) {
        viewModelStore.clear()
        super.finished(description)
    }
}
