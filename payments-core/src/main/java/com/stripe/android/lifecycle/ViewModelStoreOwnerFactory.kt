/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stripe.android.lifecycle

import androidx.annotation.RestrictTo
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState

/** Creates a [ViewModelStoreOwner] by composing a store, factory, and creation extras. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ViewModelStoreOwner(
    viewModelStore: ViewModelStore,
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = CreationExtras.Empty,
    defaultFactory: Factory = SavedStateViewModelFactory(),
): ViewModelStoreOwner {
    return object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore
            get() = viewModelStore

        override val defaultViewModelProviderFactory
            get() = defaultFactory

        override val defaultViewModelCreationExtras: CreationExtras
            get() =
                MutableCreationExtras(defaultCreationExtras).also { extras ->
                    extras[DEFAULT_ARGS_KEY] = savedState {
                        // Merge with existing arguments so upstream defaults are not overwritten
                        val existingArgs = extras[DEFAULT_ARGS_KEY]
                        if (existingArgs != null) {
                            putAll(existingArgs)
                        }
                        putAll(defaultArgs)
                    }
                    extras[VIEW_MODEL_STORE_OWNER_KEY] = this
                }
    }
}

/**
 * Creates a [ViewModelStoreOwner] that also acts as a [SavedStateRegistryOwner], composing a store,
 * state registry, lifecycle, factory, and creation extras.
 *
 * While the simpler [ViewModelStoreOwner] factory handles standard ViewModels, this overload
 * provides the necessary infrastructure for ViewModels that rely on `SavedStateHandle`. By bundling
 * the [SavedStateRegistryOwner] with the store, it creates a fully equipped scope capable of
 * properly wiring up state-saving mechanisms for its ViewModels.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun ViewModelStoreOwner(
    viewModelStore: ViewModelStore,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = CreationExtras.Empty,
    defaultFactory: Factory = SavedStateViewModelFactory(),
): ViewModelStoreOwner {
    return ViewModelStoreOwner(
        viewModelStore,
        savedStateRegistryOwner.savedStateRegistry,
        savedStateRegistryOwner.lifecycle,
        defaultArgs,
        defaultCreationExtras,
        defaultFactory,
    )
}

/**
 * Creates a [ViewModelStoreOwner] that also acts as a [SavedStateRegistryOwner], composing a store,
 * state registry, lifecycle, factory, and creation extras.
 *
 * While the simpler [ViewModelStoreOwner] factory handles standard ViewModels, this overload
 * provides the necessary infrastructure for ViewModels that rely on `SavedStateHandle`. By bundling
 * the [SavedStateRegistry] and [Lifecycle] with the store, it creates a fully equipped scope capable
 * of properly wiring up state-saving mechanisms for its ViewModels.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun ViewModelStoreOwner(
    viewModelStore: ViewModelStore,
    savedStateRegistry: SavedStateRegistry,
    lifecycle: Lifecycle,
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = CreationExtras.Empty,
    defaultFactory: Factory = SavedStateViewModelFactory(),
): ViewModelStoreOwner {
    return object :
        ViewModelStoreOwner, HasDefaultViewModelProviderFactory, SavedStateRegistryOwner {

        override val viewModelStore
            get() = viewModelStore

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistry

        override val lifecycle: Lifecycle
            get() = lifecycle

        override val defaultViewModelProviderFactory: Factory
            get() = defaultFactory

        override val defaultViewModelCreationExtras: CreationExtras
            get() =
                MutableCreationExtras(defaultCreationExtras).also { extras ->
                    extras[DEFAULT_ARGS_KEY] = savedState {
                        // Merge with existing arguments so upstream defaults are not overwritten.
                        val existingArgs = extras[DEFAULT_ARGS_KEY]
                        if (existingArgs != null) {
                            putAll(existingArgs)
                        }
                        putAll(defaultArgs)
                    }
                    extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
                    extras[VIEW_MODEL_STORE_OWNER_KEY] = this
                }

        init {
            // enableSavedStateHandles() unconditionally requires the lifecycle to still be
            // INITIALIZED or CREATED, even when registration itself would be a no-op. Since this
            // factory reuses the *host's own* registry and lifecycle (e.g. the Activity's), that
            // precondition will already be violated by the time a composable (re)mounts after the
            // host has resumed -- exactly the case this API exists to support. Guard with the same
            // short-circuit upstream androidx.lifecycle.viewmodel.ViewModelStoreOwnerFactory uses:
            // if a SavedStateProvider is already registered under this key, saved state is already
            // enabled for this registry (most commonly because the host already enabled it on
            // itself), and it's safe to skip enableSavedStateHandles() -- and its Lifecycle
            // precondition -- entirely.
            if (savedStateRegistry.getSavedStateProvider(SAVED_STATE_HANDLES_KEY) == null) {
                enableSavedStateHandles()
            }
        }
    }
}

/**
 * The key `androidx.lifecycle.SavedStateHandleSupport` registers its `SavedStateProvider` under.
 * It's `internal` to `androidx.lifecycle`, so it's duplicated here; it has been stable under this
 * exact name since Lifecycle 2.5. Remove this once this file is deleted in favor of the real
 * Lifecycle 2.11 `androidx.lifecycle.viewmodel.ViewModelStoreOwnerFactory`.
 */
private const val SAVED_STATE_HANDLES_KEY = "androidx.lifecycle.internal.SavedStateHandlesProvider"
