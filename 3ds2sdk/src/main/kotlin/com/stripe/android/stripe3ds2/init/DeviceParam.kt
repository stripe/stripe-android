package com.stripe.android.stripe3ds2.init

internal enum class DeviceParam(private val code: String) {

    // Common parameters
    PARAM_PLATFORM("C001"),
    PARAM_DEVICE_MODEL("C002"),
    PARAM_OS_NAME("C003"),
    PARAM_OS_VERSION("C004"),
    PARAM_LOCALE("C005"),
    PARAM_TIME_ZONE("C006"),
    PARAM_SCREEN_RESOLUTION("C008"),
    PARAM_DEVICE_NAME("C009"),
    PARAM_IP_ADDRESS("C010"),
    PARAM_LATITUDE("C011"),
    PARAM_LONGITUDE("C012"),
    PARAM_APP_PACKAGE_NAME("C013"),
    PARAM_SDK_APP_ID("C014"),
    PARAM_SDK_VERSION("C015"),
    PARAM_SDK_REF_NUMBER("C016"),
    PARAM_DATE_TIME("C017"),
    PARAM_SDK_TRANS_ID("C018"),

    // Android-Specific parameters

    // Telephony parameters
    PARAM_TELE_DEVICE_ID("A001"),
    PARAM_TELE_SUBSCRIBER_ID("A002"),
    PARAM_TELE_IMEI_SV("A003"),
    PARAM_TELE_GROUP_IDENTIFIER_L1("A004"),
    PARAM_TELE_LINE1_NUMBER("A005"),
    PARAM_TELE_MMS_UA_PROFILE_URL("A006"),
    PARAM_TELE_MMS_USER_AGENT("A007"),
    PARAM_TELE_NETWORK_COUNTRY_ISO("A008"),
    PARAM_TELE_NETWORK_OPERATOR("A009"),
    PARAM_TELE_NETWORK_OPERATOR_NAME("A010"),
    PARAM_TELE_NETWORK_TYPE("A011"),
    PARAM_TELE_PHONE_COUNT("A012"),
    PARAM_TELE_PHONE_TYPE("A013"),
    PARAM_TELE_SIM_COUNTRY_ISO("A014"),
    PARAM_TELE_SIM_OPERATOR("A015"),
    PARAM_TELE_SIM_OPERATOR_NAME("A016"),
    PARAM_TELE_SIM_SERIAL_NUMBER("A017"),
    PARAM_TELE_SIM_STATE("A018"),
    PARAM_TELE_VOICE_MAIL_ALPHA_TAG("A019"),
    PARAM_TELE_VOICE_MAIL_NUMBER("A020"),
    PARAM_TELE_HAS_ICC_CARD("A021"),
    PARAM_TELE_IS_HEARING_AID_COMPATIBILITY_SUPPORTED("A022"),
    PARAM_TELE_IS_NETWORK_ROAMING("A023"),
    PARAM_TELE_IS_SMS_CAPABLE("A024"),
    PARAM_TELE_IS_TTY_MODE_SUPPORTED("A025"),
    PARAM_TELE_IS_VOICE_CAPABLE("A026"),
    PARAM_TELE_IS_WORLD_PHONE("A027"),

    // Wifi parameters
    PARAM_WIFI_MAC("A028"),
    PARAM_WIFI_BSSID("A029"),
    PARAM_WIFI_SSID("A030"),
    PARAM_WIFI_NETWORK_ID("A031"),
    PARAM_WIFI_IS_5GHZ_BAND_SUPPORTED("A032"),
    PARAM_WIFI_IS_DEVICE_TO_AP_RTT_SUPPORTED("A033"),
    PARAM_WIFI_IS_ENHANCED_POWER_REPORTING_SUPPORTED("A034"),
    PARAM_WIFI_IS_P2P_SUPPORTED("A035"),
    PARAM_WIFI_IS_PREFERRED_NETWORK_OFFLOAD_SUPPORTED("A036"),
    PARAM_WIFI_IS_SCAN_ALWAYS_AVAILABLE("A037"),
    PARAM_WIFI_IS_TDLS_SUPPORTED("A038"),

    // Bluetooth parameters
    PARAM_BLUETOOTH_ADDRESS("A039"),
    PARAM_BLUETOOTH_BONDED_DEVICE("A040"),
    PARAM_BLUETOOTH_IS_ENABLED("A041"),

    // Build parameters
    PARAM_BUILD_BOARD("A042"),
    PARAM_BUILD_BOOTLOADER("A043"),
    PARAM_BUILD_BRAND("A044"),
    PARAM_BUILD_DEVICE("A045"),
    PARAM_BUILD_DISPLAY("A046"),
    PARAM_BUILD_FINGERPRINT("A047"),
    PARAM_BUILD_HARDWARE("A048"),
    PARAM_BUILD_ID("A049"),
    PARAM_BUILD_MANUFACTURER("A050"),
    PARAM_BUILD_PRODUCT("A051"),
    PARAM_BUILD_RADIO("A052"),
    PARAM_BUILD_SERIAL("A053"),
    PARAM_BUILD_SUPPORTED_32_BIT_ABIS("A054"),
    PARAM_BUILD_SUPPORTED_64_BIT_ABIS("A055"),
    PARAM_BUILD_TAGS("A056"),
    PARAM_BUILD_TIME("A057"),
    PARAM_BUILD_TYPE("A058"),
    PARAM_BUILD_USER("A059"),

    // Build Version parameters
    PARAM_BUILD_VERSION_CODENAME("A060"),
    PARAM_BUILD_VERSION_INCREMENTAL("A061"),
    PARAM_BUILD_VERSION_PREVIEW_SDK_INT("A062"),
    PARAM_BUILD_VERSION_SDK_INT("A063"),
    PARAM_BUILD_VERSION_SECURITY_PATCH("A064"),

    // Secure Settings parameters
    PARAM_SECURE_ACCESSIBILITY_DISPLAY_INVERSION_ENABLED("A065"),
    PARAM_SECURE_ACCESSIBILITY_ENABLED("A066"),
    PARAM_SECURE_ACCESSIBILITY_ACCESSIBILITY_SPEAK_PASSWORD("A067"),
    PARAM_SECURE_ALLOWED_GEOLOCATION_ORIGINS("A068"),
    PARAM_SECURE_ANDROID_ID("A069"),
    PARAM_SECURE_DATA_ROAMING("A070"),
    PARAM_SECURE_DEFAULT_INPUT_METHOD("A071"),
    PARAM_SECURE_DEVICE_PROVISIONED("A072"),
    PARAM_SECURE_ENABLED_ACCESSIBILITY_SERVICES("A073"),
    PARAM_SECURE_ENABLED_INPUT_METHODS("A074"),
    PARAM_SECURE_INPUT_METHOD_SELECTOR_VISIBILITY("A075"),
    PARAM_SECURE_INSTALL_NON_MARKET_APPS("A076"),
    PARAM_SECURE_LOCATION_MODE("A077"),
    PARAM_SECURE_SKIP_FIRST_USE_HINTS("A078"),
    PARAM_SECURE_SYS_PROP_SETTING_VERSION("A079"),
    PARAM_SECURE_TTS_DEFAULT_PITCH("A080"),
    PARAM_SECURE_TTS_DEFAULT_RATE("A081"),
    PARAM_SECURE_TTS_DEFAULT_SYNTH("A082"),
    PARAM_SECURE_TTS_ENABLED_PLUGINS("A083"),

    // Global Settings parameters
    PARAM_GLOBAL_ADB_ENABLED("A084"),
    PARAM_GLOBAL_AIRPLANE_MODE_RADIOS("A085"),
    PARAM_GLOBAL_ALWAYS_FINISH_ACTIVITIES("A086"),
    PARAM_GLOBAL_ANIMATOR_DURATION_SCALE("A087"),
    PARAM_GLOBAL_AUTO_TIME("A088"),
    PARAM_GLOBAL_AUTO_TIME_ZONE("A089"),
    PARAM_GLOBAL_DEVELOPMENT_SETTINGS_ENABLED("A090"),
    PARAM_GLOBAL_HTTP_PROXY("A091"),
    PARAM_GLOBAL_NETWORK_PREFERENCE("A092"),
    PARAM_GLOBAL_STAY_ON_WHILE_PLUGGED_IN("A093"),
    PARAM_GLOBAL_TRANSITION_ANIMATION_SCALE("A094"),
    PARAM_GLOBAL_USB_MASS_STORAGE_ENABLED("A095"),
    PARAM_GLOBAL_USE_GOOGLE_MAIL("A096"),
    PARAM_GLOBAL_WAIT_FOR_DEBUGGER("A097"),
    PARAM_GLOBAL_WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON("A098"),

    // System Setting parameters
    PARAM_SYSTEM_ACCELEROMETER_ROTATION("A099"),
    PARAM_SYSTEM_BLUETOOTH_DISCOVERABILITY("A100"),
    PARAM_SYSTEM_BLUETOOTH_DISCOVERABILITY_TIMEOUT("A101"),
    PARAM_SYSTEM_DATE_FORMAT("A102"),
    PARAM_SYSTEM_DTMF_TONE_TYPE_WHEN_DIALING("A103"),
    PARAM_SYSTEM_DTMF_TONE_WHEN_DIALING("A104"),
    PARAM_SYSTEM_END_BUTTON_BEHAVIOR("A105"),
    PARAM_SYSTEM_FONT_SCALE("A106"),
    PARAM_SYSTEM_HAPTIC_FEEDBACK_ENABLED("A107"),
    PARAM_SYSTEM_MODE_RINGER_STREAMS_AFFECTED("A108"),
    PARAM_SYSTEM_NOTIFICATION_SOUND("A109"),
    PARAM_SYSTEM_MUTE_STREAMS_AFFECTED("A110"),
    PARAM_SYSTEM_RINGTONE("A111"),
    PARAM_SYSTEM_SCREEN_BRIGHTNESS("A112"),
    PARAM_SYSTEM_SCREEN_BRIGHTNESS_MODE("A113"),
    PARAM_SYSTEM_SCREEN_OFF_TIMEOUT("A114"),
    PARAM_SYSTEM_SOUND_EFFECTS_ENABLED("A115"),
    PARAM_SYSTEM_TEXT_AUTO_CAPS("A116"),
    PARAM_SYSTEM_TEXT_AUTO_PUNCTUATE("A117"),
    PARAM_SYSTEM_TEXT_AUTO_REPLACE("A118"),
    PARAM_SYSTEM_TEXT_SHOW_PASSWORD("A119"),
    PARAM_SYSTEM_TIME_12_24("A120"),
    PARAM_SYSTEM_USER_ROTATION("A121"),
    PARAM_SYSTEM_VIBRATE_ON("A122"),
    PARAM_SYSTEM_VIBRATE_WHEN_RINGING("A123"),

    // Package Manager parameters
    PARAM_PACKAGE_IS_SAFE_MODE("A124"),
    PARAM_PACKAGE_GET_INSTALLED_APPS("A125"),
    PARAM_PACKAGE_INSTALLER_PACKAGE_NAME("A126"),
    PARAM_PACKAGE_SYSTEM_AVAILABLE_FEATURES("A127"),
    PARAM_PACKAGE_SYSTEM_SHARED_LIBRARY_NAMES("A128"),

    // Env parameters
    PARAM_ENV_EXTERNAL_STORAGE_STATE("A129"),

    // Locale parameters
    PARAM_LOCALE_AVAILABLE_LOCALES("A130"),

    // Display parameters
    PARAM_DISPLAY_DENSITY("A131"),
    PARAM_DISPLAY_DENSITY_DPI("A132"),
    PARAM_DISPLAY_SCALED_DENSITY("A133"),
    PARAM_DISPLAY_XDPI("A134"),
    PARAM_DISPLAY_YDPI("A135"),

    // StatFs parameters
    PARAM_STAT_FS_TOTAL_BYTES("A136"),

    // Web View parameters
    PARAM_WEB_VIEW_USER_AGENT("A137"),

    // SIM parameters
    PARAM_SIM_CARRIER_ID("A138"),
    PARAM_SIM_CARRIER_ID_NAME("A139"),
    PARAM_MANUFACTURER_CODE("A140"),
    PARAM_SIM_SPECIFIC_CARRIER_ID("A141"),
    PARAM_SIM_SPECIFIC_CARRIER_ID_NAME("A142"),
    PARAM_MULTI_SIM_SUPPORTED("A143"),
    PARAM_SUBSCRIPTION_ID("A145"),

    PARAM_6GHZ_BAND_SUPPORTED("A146"),
    PARAM_PASSPOINT_FQDN("A147"),
    PARAM_PASSPOINT_PROVIDER_FRIENDLY_NAME("A148"),
    PARAM_BONDED_DEVICES_ALIAS("A149"),
    PARAM_RTT_CALLING_MODE("A150"),
    PARAM_SECURE_FRP_MODE("A151"),
    PARAM_APPLY_RAMPING_RINGER("A152"),
    PARAM_HARDWARE_SKU("A153"),
    PARAM_SOC_MANUFACTURER("A154"),
    PARAM_SOC_MODEL("A155");

    override fun toString(): String {
        return code
    }
}
