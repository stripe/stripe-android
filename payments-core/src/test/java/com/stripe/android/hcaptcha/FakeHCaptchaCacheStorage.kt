package com.stripe.android.hcaptcha

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class FakeHCaptchaCacheStorage :
    HCaptchaCacheStorage,
    MutableStateFlow<CachedResult> by MutableStateFlow(CachedResult.Idle)
