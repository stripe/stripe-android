package com.stripe.android.common.nfcscan.security

import dagger.Binds
import dagger.Module
import javax.inject.Qualifier

@Module
internal interface NfcSecurityModule {
    @GlobalSettingsDevicePropertiesKey
    @Binds
    fun bindsGlobalSettingsDeviceProperties(
        deviceProperties: GlobalSettingsDeviceProperties
    ): PlatformDeviceProperties

    @OsSettingsDevicePropertiesKey
    @Binds
    fun bindsOsSettingsDeviceProperties(
        deviceProperties: OsSettingsDeviceProperties
    ): PlatformDeviceProperties

    @Binds
    fun bindsIsDeviceSecureForNfc(
        isDeviceSecureForNfc: DefaultIsDeviceSecureForNfc
    ): IsDeviceSecureForNfc
}

@Qualifier
internal annotation class GlobalSettingsDevicePropertiesKey

@Qualifier
internal annotation class OsSettingsDevicePropertiesKey
