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
    fun isSupported(publishableKey: String, isLiveMode: Boolean): Boolean

    fun start(config: CommonConfiguration, publishableKey: String, isLiveMode: Boolean)
}

internal class DefaultTapToAddConnectionStarter @Inject constructor(
    private val tapToAddConnectionManager: TapToAddConnectionManager,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val coroutineContext: CoroutineContext,
) : TapToAddConnectionStarter {
    override fun isSupported(publishableKey: String, isLiveMode: Boolean): Boolean {
        return tapToAddConnectionManager.isSupported(publishableKey, isLiveMode)
    }

    override fun start(config: CommonConfiguration, publishableKey: String, isLiveMode: Boolean) {
        viewModelScope.launch(coroutineContext) {
            runCatching {
                tapToAddConnectionManager.connect(
                    config = TapToAddConnectionManager.ConnectionConfig(
                        merchantDisplayName = config.merchantDisplayName,
                        publishableKey = publishableKey,
                        isLiveMode = isLiveMode,
                    )
                )
            }
        }
    }
}

internal class NoOpTapToAddConnectionStarter @Inject constructor() : TapToAddConnectionStarter {
    override fun isSupported(publishableKey: String, isLiveMode: Boolean): Boolean = false

    override fun start(config: CommonConfiguration, publishableKey: String, isLiveMode: Boolean) {
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
