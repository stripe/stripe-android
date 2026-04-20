package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import dagger.Binds
import dagger.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface TapToAddConnectionStarter {
    val isSupported: Boolean

    fun start(config: CommonConfiguration)
}

internal class DefaultTapToAddConnectionStarter @Inject constructor(
    private val tapToAddConnectionManager: TapToAddConnectionManager,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val coroutineContext: CoroutineContext,
) : TapToAddConnectionStarter {
    override val isSupported: Boolean
        get() = tapToAddConnectionManager.isSupported

    override fun start(config: CommonConfiguration) {
        viewModelScope.launch(coroutineContext) {
            runCatching {
                tapToAddConnectionManager.connect(
                    config = TapToAddConnectionManager.ConnectionConfig(
                        merchantDisplayName = config.merchantDisplayName,
                    )
                )
            }
        }
    }
}

internal class NoOpTapToAddConnectionStarter @Inject constructor() : TapToAddConnectionStarter {
    override val isSupported: Boolean = false

    @Suppress("UNUSED_PARAMETER")
    override fun start(config: CommonConfiguration) {
        // No-op
    }
}

@Module
internal interface TapToAddConnectionStarterModule {
    @Binds
    fun bindsTapToAddConnectionStarter(
        starter: DefaultTapToAddConnectionStarter
    ): TapToAddConnectionStarter
}

@Module
internal interface NoOpTapToAddConnectionStarterModule {
    @Binds
    fun bindsTapToAddConnectionStarter(
        starter: NoOpTapToAddConnectionStarter
    ): TapToAddConnectionStarter
}
