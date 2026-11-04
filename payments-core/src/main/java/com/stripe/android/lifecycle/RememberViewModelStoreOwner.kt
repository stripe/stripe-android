/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stripe.android.lifecycle

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.savedState

/**
 * Remembers a [ViewModelStoreOwner] scoped to the current composable.
 *
 * This function creates an owner that is unique to this specific call site in the composition
 * hierarchy. It allows creating ViewModels that are strictly scoped to this composable's lifecycle:
 * they are created when this composable enters the composition and cleared immediately when it
 * leaves.
 *
 * The owner is linked to the [parent], ensuring that configuration changes (like rotation) are
 * handled correctly: the ViewModels survive rotation if the parent does, but are destroyed if the
 * parent is destroyed.
 *
 * **Null parent:** If [parent] is **EXPLICITLY** `null`, this creates a root provider that runs
 * independently. By default, it requires a parent from the [LocalViewModelStoreOwner] and will
 * throw an [IllegalStateException] if one is not present.
 *
 * @param parent The [ViewModelStoreOwner] to use as the parent. Defaults to the owner from
 *   [LocalViewModelStoreOwner]. If this value changes, the [ViewModelStoreOwner] will be recreated.
 * @param savedStateRegistryOwner An optional [SavedStateRegistryOwner] to delegate saved state
 *   operations. When `null`, ViewModels created in this scope do not support saved state. If this
 *   value changes, the [ViewModelStoreOwner] will be recreated.
 * @param defaultArgs The [SavedState] containing default arguments to be passed to ViewModels
 *   created in this scope. These arguments are merged with any default arguments in
 *   [defaultCreationExtras]. If the same key exists in both, the value from [defaultArgs] takes
 *   precedence. This value is only read during the initial creation of the owner; subsequent
 *   recompositions with different values will not update the existing owner.
 * @param defaultCreationExtras The [CreationExtras] to use. Defaults to the [parent]'s default
 *   extras. This value is only read during the initial creation of the owner; subsequent
 *   recompositions with different values will not update or recreate the existing owner.
 * @param defaultFactory The [ViewModelProvider.Factory] to use for creating ViewModels in this
 *   scope. Defaults to the [parent]'s default factory. This value is only read during the initial
 *   creation of the owner; subsequent recompositions with different values will not update or
 *   recreate the existing owner.
 * @return A [ViewModelStoreOwner] that is remembered across compositions and scoped to this call
 *   site.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun rememberViewModelStoreOwner(
    parent: ViewModelStoreOwner? =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "CompositionLocal LocalViewModelStoreOwner not present"
        },
    savedStateRegistryOwner: SavedStateRegistryOwner? = LocalSavedStateRegistryOwner.current,
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = parent.defaultViewModelCreationExtras,
    defaultFactory: ViewModelProvider.Factory = parent.defaultViewModelProviderFactory,
): ViewModelStoreOwner {
    val key = currentCompositeKeyHashCode
    val provider =
        rememberViewModelStoreProvider(
            key,
            parent,
            defaultArgs,
            defaultCreationExtras,
            defaultFactory,
        )
    return rememberViewModelStoreOwner(
        provider = provider,
        key = key,
        savedStateRegistryOwner = savedStateRegistryOwner,
    )
}

/**
 * Remembers a [ViewModelStoreOwner] scoped to the current composable using an existing [provider]
 * and a specific [key].
 *
 * This function allows you to scope a [ViewModelStoreOwner] to a custom [key]. This is useful in
 * cases where using a key in different parts of the UI should yield the same state or instance
 * (similar to how a ViewModel is shared). For example, you might use a key derived from navigation
 * arguments, such as `ViewModelNavEntryDecorator.contentKey`, to share the same owner across
 * different screens or components.
 *
 * **Note:** Unlike many other scoped owners, ViewModels created with this owner are **not**
 * automatically cleared simply because this composable leaves the composition. The [ViewModelStore]
 * is only cleared when [ViewModelStoreProvider.clearKey] is explicitly called for this [key].
 *
 * This function is responsible for releasing its reference to the store when it leaves the
 * composition, allowing the [provider] to perform cleanup if the store has been marked for
 * clearing.
 *
 * @param key A unique identifier to isolate this store from others. Providing the same [key] and
 *   [provider] to multiple [rememberViewModelStoreOwner] calls will yield the same
 *   [ViewModelStoreOwner] and shared state. If this value changes, the [ViewModelStoreOwner] will
 *   be recreated.
 * @param provider The [ViewModelStoreProvider] that manages the creation and cleanup of the
 *   underlying [ViewModelStore]. If this value changes, the [ViewModelStoreOwner] will be
 *   recreated.
 * @param savedStateRegistryOwner An optional [SavedStateRegistryOwner] to delegate saved state
 *   operations. When `null`, ViewModels created in this scope do not support saved state. If this
 *   value changes, the [ViewModelStoreOwner] will be recreated.
 * @return A [ViewModelStoreOwner] remembered across compositions and scoped to the provided [key].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
private fun rememberViewModelStoreOwner(
    key: Any?,
    provider: ViewModelStoreProvider,
    savedStateRegistryOwner: SavedStateRegistryOwner? = LocalSavedStateRegistryOwner.current,
): ViewModelStoreOwner {
    val owner =
        remember(provider, key, savedStateRegistryOwner) {
            provider.getOrCreateOwner(key, savedStateRegistryOwner)
        }
    DisposableEffect(owner) {
        val token = provider.acquireToken(key)
        onDispose { token.close() }
    }
    return owner
}

@Composable
private fun rememberViewModelStoreProvider(
    key: Any?,
    parent: ViewModelStoreOwner? =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "CompositionLocal LocalViewModelStoreOwner not present"
        },
    defaultArgs: SavedState = savedState(),
    defaultCreationExtras: CreationExtras = parent.defaultViewModelCreationExtras,
    defaultFactory: ViewModelProvider.Factory = parent.defaultViewModelProviderFactory,
): ViewModelStoreProvider {
    val provider =
        remember(parent, key) {
            ViewModelStoreProvider(parent, key, defaultArgs, defaultCreationExtras, defaultFactory)
        }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(provider, lifecycle) {
        val token = provider.acquireToken(ViewModelStoreProvider.ProviderMarkerKey)
        onDispose {
            token.close()
            // We are NOT waiting for an ON_DESTROY event, instead we are executing a cleanup hook
            // that is guaranteed to run when this composable leaves the tree, and checking the
            // Parent's current state to decide *if* we should dispose.
            //
            // If the Parent Lifecycle is DESTROYED, it implies the Activity is either:
            // 1. Rotating (Configuration Change): We MUST NOT dispose, so the data survives.
            // 2. Finishing: The Parent's own ViewModelStore will clear everything anyway.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                provider.clearAllKeys()
            }
        }
    }
    return provider
}
