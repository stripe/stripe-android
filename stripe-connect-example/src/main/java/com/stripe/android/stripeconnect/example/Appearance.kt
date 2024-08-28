package com.stripe.android.stripeconnect.example

import com.stripe.android.stripeconnect.Appearance
import kotlinx.parcelize.Parcelize

@Parcelize
data class PurpleHazeAppearance(
    override val colorPrimary: Int = 0x414AC3,
    override val colorBackground: Int = 0xE0C3FC,
    override val badgeNeutralColorBackground: Int? = 0xF2E8F8,
    override val badgeSuccessColorBackground: Int? = 0x8E94F2,
    override val badgeSuccessColorBorder: Int? = 0xFFFFFF,
    override val badgeWarningColorBackground: Int? = 0xFFC2E2,
    override val badgeWarningColorBorder: Int? = 0xFFFFFF,
    override val badgeDangerColorBackground: Int? = 0xEF7A85,
    override val badgeDangerColorBorder: Int? = 0xFFFFFF,
    override val borderRadius: Int? = 24,
    override val spacingUnit: Int? = 9,
    override val horizontalPadding: Int? = 8,
    override val fontFamily: String? = "Yellowtail"
) : Appearance()

@Parcelize
data class OgreAppearance(
    override val colorPrimary: Int = 0x5AE92B,
    override val colorText: Int? = 0x554125,
    override val colorBackground: Int = 0x837411,
    override val buttonPrimaryColorBackground: Int? = 0xDAB9B9,
    override val buttonPrimaryColorBorder: Int? = 0xF00000,
    override val buttonPrimaryColorText: Int? = 0x000000,
    override val buttonSecondaryColorBackground: Int? = 0x025F08,
    override val buttonSecondaryColorText: Int? = 0x000000,
    override val badgeNeutralColorBackground: Int? = 0x638863,
    override val badgeNeutralColorText: Int? = 0x28D72A,
    override val fontFamily: String? = "Yatra One"
) : Appearance()

@Parcelize
data class ProtanopiaAppearance(
    override val colorPrimary: Int = 0x0969DA,
    override val colorText: Int? = 0x24292F,
    override val colorBackground: Int = 0xFFFFFF,
    override val buttonPrimaryColorBackground: Int? = 0x0969DA,
    override val buttonPrimaryColorBorder: Int? = 0x1B1F24,
    override val buttonPrimaryColorText: Int? = 0xFFFFFF,
    override val buttonSecondaryColorBackground: Int? = 0xF6F8FA,
    override val buttonSecondaryColorBorder: Int? = 0x1B1F24,
    override val buttonSecondaryColorText: Int? = 0x24292F,
    override val spacingUnit: Int? = 8,
) : Appearance()

@Parcelize
data class OceanBreezeAppearance(
    override val colorPrimary: Int = 0x15609E,
    override val colorBackground: Int = 0xEAF6FB,
    override val buttonSecondaryColorBorder: Int? = 0x2C93E8,
    override val buttonSecondaryColorText: Int? = 0x2C93E8,
    override val badgeNeutralColorText: Int? = 0x5A621D,
    override val badgeSuccessColorText: Int? = 0x2A6093,
    override val borderRadius: Int? = 23,
    override val horizontalPadding: Int? = 50
) : Appearance()

@Parcelize
data class HotDogAppearance(
    override val colorPrimary: Int = 0xFF2200,
    override val colorText: Int? = 0x000000,
    override val colorBackground: Int = 0xFFFF00,
    override val buttonPrimaryColorBackground: Int? = 0xC6C6C6,
    override val buttonPrimaryColorBorder: Int? = 0x1F1F1F,
    override val buttonPrimaryColorText: Int? = 0x1F1F1F,
    override val buttonSecondaryColorBackground: Int? = 0xC6C6C6,
    override val buttonSecondaryColorBorder: Int? = 0x1F1F1F,
    override val badgeWarningColorBackground: Int? = 0xF9A443,
    override val badgeDangerColorText: Int? = 0x991400,
    override val borderRadius: Int? = 0,
    override val fontFamily: String? = "Gill Sans"
) : Appearance()

@Parcelize
data class JazzCupAppearance(
    override val colorPrimary: Int = 0x2C1679,
    override val colorText: Int? = 0x2C1679,
    override val buttonSecondaryColorBackground: Int? = 0x0CCDDB,
    override val buttonSecondaryColorText: Int? = 0x2C1679,
    override val fontFamily: String? = "Chalkboard SE"
) : Appearance()

@Parcelize
data class PolarVortex(
    override val fontFamily: String? = "PolarVortex"
) : Appearance()

val appearances = listOf(
    PurpleHazeAppearance() to "Purple Haze",
    OgreAppearance() to "Ogre",
    ProtanopiaAppearance() to "Protanopia",
    OceanBreezeAppearance() to "Ocean Breeze",
    HotDogAppearance() to "Hot Dog",
    JazzCupAppearance() to "Jazz Cup",
    PolarVortex() to "Polar Vortex",
    null to "Default",
)