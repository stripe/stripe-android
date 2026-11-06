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

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Returns the appropriate default [ViewModelProvider.Factory] to use.
 *
 * If the provided [ViewModelStoreOwner] implements [HasDefaultViewModelProviderFactory], this will
 * return its [HasDefaultViewModelProviderFactory.defaultViewModelProviderFactory]. Otherwise, or if
 * [ViewModelStoreOwner] is `null`, it falls back to [ViewModelProvider.NewInstanceFactory].
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val ViewModelStoreOwner?.defaultViewModelCreationExtras: CreationExtras
    get() {
        val owner = this
        return if (owner is HasDefaultViewModelProviderFactory) {
            owner.defaultViewModelCreationExtras
        } else {
            CreationExtras.Empty
        }
    }

/**
 * Returns the appropriate default [CreationExtras] to use.
 *
 * If the provided [ViewModelStoreOwner] implements [HasDefaultViewModelProviderFactory], this will
 * return its [HasDefaultViewModelProviderFactory.defaultViewModelCreationExtras]. Otherwise, or if
 * [ViewModelStoreOwner] is `null`, it falls back to [CreationExtras.Empty].
 */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val ViewModelStoreOwner?.defaultViewModelProviderFactory: ViewModelProvider.Factory
    @SuppressLint("RestrictedApi")
    get() {
        val owner = this
        return if (owner is HasDefaultViewModelProviderFactory) {
            owner.defaultViewModelProviderFactory
        } else {
            ViewModelProvider.NewInstanceFactory.instance
        }
    }
