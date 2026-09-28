package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.taptoadd.TapToAddConnectionManager
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import dagger.Binds
import dagger.Module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface TapToAddConnectionStarter {
    fun isSupported(apiConfiguration: ApiConfiguration.State): Boolean

    fun start(config: CommonConfiguration, apiConfiguration: ApiConfiguration.State)
}

internal class DefaultTapToAddConnectionStarter @Inject constructor(
    private val tapToAddConnectionManager: TapToAddConnectionManager,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val coroutineContext: CoroutineContext,
) : TapToAddConnectionStarter {
    override fun isSupported(apiConfiguration: ApiConfiguration.State): Boolean {
        return tapToAddConnectionManager.isSupported(apiConfiguration)
    }

    override fun start(config: CommonConfiguration, apiConfiguration: ApiConfiguration.State) {
        viewModelScope.launch(coroutineContext) {
            runCatching {
                tapToAddConnectionManager.connect(
                    config = TapToAddConnectionManager.ConnectionConfig(
                        merchantDisplayName = config.merchantDisplayName,
                        apiConfiguration = apiConfiguration,
                    )
                )
            }
        }
    }
}

internal class NoOpTapToAddConnectionStarter @Inject constructor() : TapToAddConnectionStarter {
    override fun isSupported(apiConfiguration: ApiConfiguration.State): Boolean = false

    override fun start(config: CommonConfiguration, apiConfiguration: ApiConfiguration.State) {
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
