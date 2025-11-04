package com.stripe.android.uicore.elements

/*
 * Emoji presentation unicode symbols from Unicode 16.0.0
 * https://www.unicode.org/Public/16.0.0/ucd/emoji/emoji-data.txt
 *
 * If the minimum Java version required by the SDK is ever set to 21,
 * use the `\p{IsEmoji_Presentation}` pattern instead.
 */
private const val EmojiPresentation =
    "[" +
        "\\u231A-\\u231B" +
        "\\u23E9-\\u23EC" +
        "\\u23F0" +
        "\\u23F3" +
        "\\u25FD-\\u25FE" +
        "\\u2614-\\u2615" +
        "\\u2648-\\u2653" +
        "\\u267F" +
        "\\u2693" +
        "\\u26A1" +
        "\\u26AA-\\u26AB" +
        "\\u26BD-\\u26BE" +
        "\\u26C4-\\u26C5" +
        "\\u26CE" +
        "\\u26D4" +
        "\\u26EA" +
        "\\u26F2-\\u26F3" +
        "\\u26F5" +
        "\\u26FA" +
        "\\u26FD" +
        "\\u2705" +
        "\\u270A-\\u270B" +
        "\\u2728" +
        "\\u274C" +
        "\\u274E" +
        "\\u2753-\\u2755" +
        "\\u2757" +
        "\\u2795-\\u2797" +
        "\\u27B0" +
        "\\u27BF" +
        "\\u2B1B-\\u2B1C" +
        "\\u2B50" +
        "\\u2B55" +
        "\\uD83C\\uDC04" +
        "\\uD83C\\uDCCF" +
        "\\uD83C\\uDD8E" +
        "\\uD83C\\uDD91-\\uD83C\\uDD9A" +
        "\\uD83C\\uDDE6-\\uD83C\\uDDFF" +
        "\\uD83C\\uDE01" +
        "\\uD83C\\uDE1A" +
        "\\uD83C\\uDE2F" +
        "\\uD83C\\uDE32-\\uD83C\\uDE36" +
        "\\uD83C\\uDE38-\\uD83C\\uDE3A" +
        "\\uD83C\\uDE50-\\uD83C\\uDE51" +
        "\\uD83C\\uDF00-\\uD83C\\uDFF4" +
        "\\uD83C\\uDFF8-\\uD83D\\uDD3D" +
        "\\uD83D\\uDD4B-\\uD83D\\uDDA4" +
        "\\uD83D\\uDDFB-\\uD83D\\uDE4F" +
        "\\uD83D\\uDE80-\\uD83D\\uDFF0" +
        "\\uD83E\\uDD0C-\\uD83E\\uDDFF" +
        "\\uD83E\\uDE70-\\uD83E\\uDEF8" +
        "]|" +
        "[\\u0023\\u002A\\u0030-\\u0039]\\uFE0F\\u20E3"

/*
 * Extended pictographic unicode symbols from Unicode 16.0.0
 * https://www.unicode.org/Public/16.0.0/ucd/emoji/emoji-data.txt
 *
 * If the minimum Java version required by the SDK is ever set to 21,
 * use the `\p{IsExtended_Pictographic}` pattern instead.
 */
private const val ExtendedPictographic =
    "[" +
        "\\u00A9" +
        "\\u00AE" +
        "\\u203C" +
        "\\u2049" +
        "\\u2122" +
        "\\u2139" +
        "\\u2194-\\u2199" +
        "\\u21A9-\\u21AA" +
        "\\u231A-\\u231B" +
        "\\u2328" +
        "\\u2388" +
        "\\u23CF" +
        "\\u23E9-\\u23FA" +
        "\\u24C2" +
        "\\u25AA-\\u25AB" +
        "\\u25B6" +
        "\\u25C0" +
        "\\u25FB-\\u25FE" +
        "\\u2600-\\u2605" +
        "\\u2607-\\u2612" +
        "\\u2614-\\u2685" +
        "\\u2690-\\u2705" +
        "\\u2708-\\u2712" +
        "\\u2714" +
        "\\u2716" +
        "\\u271D" +
        "\\u2721" +
        "\\u2728" +
        "\\u2733-\\u2734" +
        "\\u2744" +
        "\\u2747" +
        "\\u274C" +
        "\\u274E" +
        "\\u2753-\\u2755" +
        "\\u2757" +
        "\\u2763-\\u2764" +
        "\\u2765-\\u2767" +
        "\\u2795-\\u2797" +
        "\\u27A1" +
        "\\u27B0" +
        "\\u27BF" +
        "\\u2934-\\u2935" +
        "\\u2B05-\\u2B07" +
        "\\u2B1B-\\u2B1C" +
        "\\u2B50" +
        "\\u2B55" +
        "\\u3030" +
        "\\u303D" +
        "\\u3297" +
        "\\u3299" +
        "]|" +
        "[\\uD83C\\uDC00-\\uD83F\\uDFFD]" +
        "]"

/*
 * Invisible unicode character that forces the previous unicode character to use color when being displayed.
 */
private const val VariantSelector16 = "\\uFE0F"

/*
 * Follows stripe-js-v3 pattern of filtering out emojis from addresses.
 */
internal val EMOJI_REGEX = Regex("$EmojiPresentation|$ExtendedPictographic|$VariantSelector16")
