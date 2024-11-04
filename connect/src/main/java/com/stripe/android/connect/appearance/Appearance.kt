package com.stripe.android.connect.appearance

import android.os.Parcelable
import com.stripe.android.connect.PrivateBetaConnectSDK
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@Parcelize
@Poko
class Appearance(
    /**
     * Describes the colors used while the system is in light mode.
     */
    val colorsLight: Colors = Colors.default,

    /**
     * Describes the colors used while the system is in dark mode, or null if dark mode is not supported. Dark mode is not supported by default.
     */
    val colorsDark: Colors? = null,

    /**
     * Describes the corner radius used in embedded components.
     */
    val cornerRadius: CornerRadius = CornerRadius.default,

    /**
     * Describes the typography used for text.
     */
    val typography: Typography = Typography.default,

    /**
     * Describes the primary button appearance settings when in light mode.
     */
    val buttonPrimaryLight: Button = Button.default,

    /**
     * Describes the primary button appearance settings when in dark mode. If null then dark mode is not supported.
     */
    val buttonPrimaryDark: Button? = null,

    /**
     * Describes the secondary button appearance settings when in light mode.
     */
    val buttonSecondaryLight: Button = Button.default,

    /**
     * Describes the secondary button appearance settings when in dark mode. If null then dark mode is not supported.
     */
    val buttonSecondaryDark: Button? = null,

    /**
     * Describes the neutral badge appearance settings.
     */
    val badgeNeutralLight: Badge = Badge.default,

    /**
     * Describes the neutral badge appearance settings. If null then dark mode is not supported.
     */
    val badgeNeutralDark: Badge? = null,

    /**
     * Describes the success badge appearance settings when in light mode.
     */
    val badgeSuccessLight: Badge = Badge.default,

    /**
     * Describes the success badge appearance settings when in dark mode. If null then dark mode is not supported.

     */
    val badgeSuccessDark: Badge? = null,

    /**
     * Describes the warning badge appearance settings when in light mode.
     */
    val badgeWarningLight: Badge = Badge.default,

    /**
     * Describes the warning badge appearance settings when in dark mode. If null then dark mode is not supported.
     */
    val badgeWarningDark: Badge? = null,

    /**
     * Describes the danger badge appearance settings when in light mode.
     */
    val badgeDangerLight: Badge = Badge.default,

    /**
     * Describes the danger badge appearance settings when in dark mode. If null then dark mode is not supported.

     */
    val badgeDangerDark: Badge? = null,
) : Parcelable
