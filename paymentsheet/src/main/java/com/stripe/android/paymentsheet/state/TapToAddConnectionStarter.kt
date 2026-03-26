package com.stripe.android.paymentsheet.state

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

    fun start()
}

internal class DefaultTapToAddConnectionStarter @Inject constructor(
    private val tapToAddConnectionManager: TapToAddConnectionManager,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    @IOContext private val coroutineContext: CoroutineContext,
) : TapToAddConnectionStarter {
    override val isSupported: Boolean
        get() = tapToAddConnectionManager.isSupported

    override fun start() {
        viewModelScope.launch(coroutineContext) {
            runCatching {
                tapToAddConnectionManager.connect()
            }
        }
    }
}

internal class NoOpTapToAddConnectionStarter @Inject constructor() : TapToAddConnectionStarter {
    override val isSupported: Boolean = false

    override fun start() {
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
