package com.stripe.android.uicore.elements

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.Build
import androidx.annotation.RequiresApi

@SuppressWarnings("MagicNumber")
internal class AddressTextFilter : TextFilter {
    override fun filter(text: String): String {
        return text.withoutEmojis()
    }

    private fun String.withoutEmojis(): String {
        val out = StringBuilder(length)
        var i = 0
        while (i < length) {
            val cp = codePointAt(i)
            val charCount = Character.charCount(cp)

            // Check if this is part of a keycap sequence (digit/# followed by variation selector + keycap)
            val isKeycapSequence = i + charCount < length &&
                (cp in 0x0030..0x0039 || cp == 0x0023) && // 0-9 or #
                hasKeycapSequence(i + charCount)

            if (!isKeycapSequence && !cp.isEmojiCodePoint()) {
                out.appendCodePoint(cp)
            } else if (isKeycapSequence) {
                // Skip the entire keycap sequence (base + variation selector + keycap)
                i += charCount
                if (i < length && this[i].code == 0xFE0F) i++ // variation selector
                if (i < length && this[i].code == 0x20E3) i++ // keycap
                continue
            }

            i += charCount
        }
        return out.toString()
    }

    private fun String.hasKeycapSequence(startIndex: Int): Boolean {
        // Check for variation selector (optional) + keycap combiner
        var idx = startIndex
        if (idx < length && this[idx].code == 0xFE0F) {
            idx++
        }
        return idx < length && this[idx].code == 0x20E3
    }

    private fun Int.isEmojiCodePoint(): Boolean {
        if (isEmojiModifierOrCombiner()) return true
        if (isCommonEmojiRange()) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return isEmojiViaICU()
        }
        return isEmojiViaUnicodeBlock()
    }

    private fun Int.isEmojiModifierOrCombiner(): Boolean {
        // ZWJ, keycap, variation selectors, VS supplement, skin tones, tags
        return this == 0x200D || this == 0x20E3 ||
            this in 0xFE00..0xFE0F ||
            this in 0xE0100..0xE01EF ||
            this in 0x1F3FB..0x1F3FF ||
            this in 0xE0000..0xE007F
    }

    private fun Int.isCommonEmojiRange(): Boolean {
        // Common emoji ranges (check these on all API levels for consistency)
        return this in 0x2600..0x26FF || // Miscellaneous Symbols (☀-⛿)
            this in 0x2700..0x27BF || // Dingbats (✀-➿)
            this in 0x2B00..0x2BFF || // Miscellaneous Symbols and Arrows (⬀-⯿) including ⭐
            this in 0x1F600..0x1F64F || // Emoticons (😀-🙏)
            this in 0x1F300..0x1F5FF || // Miscellaneous Symbols and Pictographs (🌀-🗿)
            this in 0x1F680..0x1F6FF || // Transport and Map Symbols (🚀-🛿)
            this in 0x1F900..0x1F9FF || // Supplemental Symbols and Pictographs (🤪, 🦰, etc.)
            this in 0x1F1E0..0x1F1FF // Regional Indicator Symbols (🇠-🇿) for flags
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun Int.isEmojiViaICU(): Boolean {
        // Exclude ASCII characters that are only emojis as part of keycap sequences
        // 0-9 (0x0030-0x0039), # (0x0023), * (0x002A)
        if (this in 0x0030..0x0039 || this == 0x0023 || this == 0x002A) {
            return false
        }
        return UCharacter.hasBinaryProperty(this, UProperty.EMOJI)
    }

    private fun Int.isEmojiViaUnicodeBlock(): Boolean {
        val block = Character.UnicodeBlock.of(this)
        return when (block) {
            Character.UnicodeBlock.EMOTICONS,
            Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS,
            Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS,
            Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS,
            Character.UnicodeBlock.DINGBATS,
            Character.UnicodeBlock.ENCLOSED_ALPHANUMERICS,
            Character.UnicodeBlock.ENCLOSED_ALPHANUMERIC_SUPPLEMENT,
            Character.UnicodeBlock.ENCLOSED_IDEOGRAPHIC_SUPPLEMENT
            -> true
            else -> isSupplementalSymbolsAndPictographs(block)
        }
    }

    private fun Int.isSupplementalSymbolsAndPictographs(block: Character.UnicodeBlock?): Boolean {
        // SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS requires API 34+
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            block == Character.UnicodeBlock.SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
        } else {
            // Fallback: check if codepoint is in the range for this block (U+1F900–U+1F9FF)
            this in 0x1F900..0x1F9FF
        }
    }
}
