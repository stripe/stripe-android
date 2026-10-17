package com.stripe.android.connect.appearance

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class Appearance private constructor(
    internal val colors: Colors,
    internal val cornerRadius: CornerRadius,
    internal val typography: Typography,
    internal val spacingUnit: Float?,
    internal val buttonPrimary: Button,
    internal val buttonSecondary: Button,
    internal val buttonDanger: Button,
    internal val buttonDefaults: ButtonDefaults,
    internal val badgeNeutral: Badge,
    internal val badgeSuccess: Badge,
    internal val badgeWarning: Badge,
    internal val badgeDanger: Badge,
    internal val badgeDefaults: BadgeDefaults,
    internal val actionPrimaryText: Action,
    internal val actionSecondaryText: Action,
    internal val form: Form,
    internal val tableRowPaddingY: Float?
) : Parcelable {

    @SuppressWarnings("TooManyFunctions")
    class Builder {
        private var colors: Colors = Colors.default()
        private var cornerRadius: CornerRadius = CornerRadius.default()
        private var typography: Typography = Typography.default()
        private var spacingUnit: Float? = null
        private var buttonPrimary: Button = Button.default()
        private var buttonSecondary: Button = Button.default()
        private var buttonDanger: Button = Button.default()
        private var buttonDefaults: ButtonDefaults = ButtonDefaults.default()
        private var badgeNeutral: Badge = Badge.default()
        private var badgeSuccess: Badge = Badge.default()
        private var badgeWarning: Badge = Badge.default()
        private var badgeDanger: Badge = Badge.default()
        private var badgeDefaults: BadgeDefaults = BadgeDefaults.default()
        private var actionPrimaryText: Action = Action.default()
        private var actionSecondaryText: Action = Action.default()
        private var form: Form = Form.default()
        private var tableRowPaddingY: Float? = null

        /**
         * Describes the colors used in embedded components.
         */
        fun colors(colors: Colors): Builder =
            apply { this.colors = colors }

        /**
         * Describes the corner radius used in embedded components.
         */
        fun cornerRadius(cornerRadius: CornerRadius): Builder =
            apply { this.cornerRadius = cornerRadius }

        /**
         * Describes the typography used for text.
         */
        fun typography(typography: Typography): Builder =
            apply { this.typography = typography }

        /**
         * The base spacing unit that derives all spacing values.
         * Increase or decrease this value to make your layout more or less spacious.
         */
        fun spacingUnit(spacingUnit: Float): Builder =
            apply { this.spacingUnit = spacingUnit }

        /**
         * Describes the primary button appearance settings.
         */
        fun buttonPrimary(buttonPrimary: Button): Builder =
            apply { this.buttonPrimary = buttonPrimary }

        /**
         * Describes the secondary button appearance settings.
         */
        fun buttonSecondary(buttonSecondary: Button): Builder =
            apply { this.buttonSecondary = buttonSecondary }

        /**
         * Describes the danger button appearance settings.
         */
        fun buttonDanger(buttonDanger: Button): Builder =
            apply { this.buttonDanger = buttonDanger }

        /**
         * Describes the padding and label typography shared by all button variants.
         */
        fun buttonDefaults(buttonDefaults: ButtonDefaults): Builder =
            apply { this.buttonDefaults = buttonDefaults }

        /**
         * Describes the neutral badge appearance settings.
         */
        fun badgeNeutral(badgeNeutral: Badge): Builder =
            apply { this.badgeNeutral = badgeNeutral }

        /**
         * Describes the success badge appearance settings.
         */
        fun badgeSuccess(badgeSuccess: Badge): Builder =
            apply { this.badgeSuccess = badgeSuccess }

        /**
         * Describes the warning badge appearance settings.
         */
        fun badgeWarning(badgeWarning: Badge): Builder =
            apply { this.badgeWarning = badgeWarning }

        /**
         * Describes the danger badge appearance settings.
         */
        fun badgeDanger(badgeDanger: Badge): Builder =
            apply { this.badgeDanger = badgeDanger }

        /**
         * Describes the padding and label typography shared by all badge variants.
         */
        fun badgeDefaults(badgeDefaults: BadgeDefaults): Builder =
            apply { this.badgeDefaults = badgeDefaults }

        /**
         * Describes the primary action text appearance settings.
         */
        fun actionPrimaryText(actionPrimaryText: Action): Builder =
            apply { this.actionPrimaryText = actionPrimaryText }

        /**
         * Describes the secondary action text appearance settings.
         */
        fun actionSecondaryText(actionSecondaryText: Action): Builder =
            apply { this.actionSecondaryText = actionSecondaryText }

        /**
         * Describes the form appearance settings.
         */
        fun form(form: Form): Builder =
            apply { this.form = form }

        /**
         * Describes the vertical table row padding appearance settings.
         */
        fun tableRowPaddingY(tableRowPaddingY: Float): Builder =
            apply { this.tableRowPaddingY = tableRowPaddingY }

        fun build(): Appearance {
            return Appearance(
                colors = colors,
                cornerRadius = cornerRadius,
                typography = typography,
                spacingUnit = spacingUnit,
                buttonPrimary = buttonPrimary,
                buttonSecondary = buttonSecondary,
                buttonDanger = buttonDanger,
                buttonDefaults = buttonDefaults,
                badgeNeutral = badgeNeutral,
                badgeSuccess = badgeSuccess,
                badgeWarning = badgeWarning,
                badgeDanger = badgeDanger,
                badgeDefaults = badgeDefaults,
                actionPrimaryText = actionPrimaryText,
                actionSecondaryText = actionSecondaryText,
                form = form,
                tableRowPaddingY = tableRowPaddingY,
            )
        }
    }

    internal companion object {
        internal fun default() = Builder().build()
    }
}
