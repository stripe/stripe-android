package com.stripe.android.connect.appearance

import android.os.Parcelable
import com.stripe.android.connect.PrivateBetaConnectSDK
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@PrivateBetaConnectSDK
@Parcelize
@Poko
class Appearance private constructor(
    internal val colors: Colors,
    internal val cornerRadius: CornerRadius,
    internal val typography: Typography,
    internal val buttonPrimary: Button,
    internal val buttonSecondary: Button,
    internal val badgeNeutral: Badge,
    internal val badgeSuccess: Badge,
    internal val badgeWarning: Badge,
    internal val badgeDanger: Badge,
) : Parcelable {

    class Builder {
        private var colors: Colors = Colors.default()
        private var cornerRadius: CornerRadius = CornerRadius.default()
        private var typography: Typography = Typography.default()
        private var buttonPrimary: Button = Button.default()
        private var buttonSecondary: Button = Button.default()
        private var badgeNeutral: Badge = Badge.default()
        private var badgeSuccess: Badge = Badge.default()
        private var badgeWarning: Badge = Badge.default()
        private var badgeDanger: Badge = Badge.default()

        /**
         * Describes the colors used in embedded components.
         */
        fun setColors(colors: Colors) =
            apply { this.colors = colors }

        /**
         * Describes the corner radius used in embedded components.
         */
        fun setCornerRadius(cornerRadius: CornerRadius) =
            apply { this.cornerRadius = cornerRadius }

        /**
         * Describes the typography used for text.
         */
        fun setTypography(typography: Typography) =
            apply { this.typography = typography }

        /**
         * Describes the primary button appearance settings.
         */
        fun setButtonPrimary(buttonPrimary: Button) =
            apply { this.buttonPrimary = buttonPrimary }

        /**
         * Describes the secondary button appearance settings.
         */
        fun setButtonSecondary(buttonSecondary: Button) =
            apply { this.buttonSecondary = buttonSecondary }

        /**
         * Describes the neutral badge appearance settings.
         */
        fun setBadgeNeutral(badgeNeutral: Badge) =
            apply { this.badgeNeutral = badgeNeutral }

        /**
         * Describes the success badge appearance settings.
         */
        fun setBadgeSuccess(badgeSuccess: Badge) =
            apply { this.badgeSuccess = badgeSuccess }

        /**
         * Describes the warning badge appearance settings.
         */
        fun setBadgeWarning(badgeWarning: Badge) =
            apply { this.badgeWarning = badgeWarning }

        /**
         * Describes the danger badge appearance settings.
         */
        fun setBadgeDanger(badgeDanger: Badge) =
            apply { this.badgeDanger = badgeDanger }

        fun build(): Appearance {
            return Appearance(
                colors = colors,
                cornerRadius = cornerRadius,
                typography = typography,
                buttonPrimary = buttonPrimary,
                buttonSecondary = buttonSecondary,
                badgeNeutral = badgeNeutral,
                badgeSuccess = badgeSuccess,
                badgeWarning = badgeWarning,
                badgeDanger = badgeDanger
            )
        }
    }

    internal companion object {
        internal fun default() = Builder().build()
    }
}
