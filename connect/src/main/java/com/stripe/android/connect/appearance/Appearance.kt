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
     * Describes the colors used in embedded components.
     */
    val colors: Colors = Colors.default,

    /**
     * Describes the corner radius used in embedded components.
     */
    val cornerRadius: CornerRadius = CornerRadius.default,

    /**
     * Describes the typography used for text.
     */
    val typography: Typography = Typography.default,

    /**
     * Describes the primary button appearance settings.
     */
    val buttonPrimary: Button = Button.default,

    /**
     * Describes the secondary button appearance settings.
     */
    val buttonSecondary: Button = Button.default,

    /**
     * Describes the neutral badge appearance settings.
     */
    val badgeNeutral: Badge = Badge.default,

    /**
     * Describes the success badge appearance settings.
     */
    val badgeSuccess: Badge = Badge.default,

    /**
     * Describes the warning badge appearance settings.
     */
    val badgeWarning: Badge = Badge.default,

    /**
     * Describes the danger badge appearance settings.
     */
    val badgeDanger: Badge = Badge.default,
) : Parcelable
