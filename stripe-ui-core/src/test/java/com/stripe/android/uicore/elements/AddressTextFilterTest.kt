package com.stripe.android.uicore.elements

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [
        Build.VERSION_CODES.LOLLIPOP, // API 21 - Min SDK, uses fallback
        Build.VERSION_CODES.N, // API 24 - Still uses fallback
        Build.VERSION_CODES.O_MR1, // API 27 - Last before ICU support
        Build.VERSION_CODES.P, // API 28 - UProperty.EMOJI available
        Build.VERSION_CODES.Q, // API 29
        Build.VERSION_CODES.TIRAMISU, // API 33 - Before SUPPLEMENTAL_SYMBOLS_AND_PICTOGRAPHS
        Build.VERSION_CODES.UPSIDE_DOWN_CAKE // API 34 - Full support
    ]
)
internal class AddressTextFilterTest {

    private val filter = AddressTextFilter()

    @Test
    fun `filter returns text unchanged when no emojis present`() {
        assertThat(filter.filter("123 Main Street")).isEqualTo("123 Main Street")
        assertThat(filter.filter("Apartment 5B")).isEqualTo("Apartment 5B")
        assertThat(filter.filter("New York, NY 10001")).isEqualTo("New York, NY 10001")
        assertThat(filter.filter("Suite 100-A")).isEqualTo("Suite 100-A")
    }

    @Test
    fun `filter removes common face emojis`() {
        assertThat(filter.filter("123 Main St 😀")).isEqualTo("123 Main St ")
        assertThat(filter.filter("😊 Happy Street")).isEqualTo(" Happy Street")
        assertThat(filter.filter("Apt 😎 5")).isEqualTo("Apt  5")
        assertThat(filter.filter("😀😊😎")).isEqualTo("")
    }

    @Test
    fun `filter removes various emoji types`() {
        // Heart and symbols
        assertThat(filter.filter("Love ❤️ Street")).isEqualTo("Love  Street")
        // Thumbs up
        assertThat(filter.filter("Good 👍 Road")).isEqualTo("Good  Road")
        // Fire
        assertThat(filter.filter("Hot 🔥 Avenue")).isEqualTo("Hot  Avenue")
        // Star
        assertThat(filter.filter("Star ⭐ Boulevard")).isEqualTo("Star  Boulevard")
    }

    @Test
    fun `filter removes transport and map emojis`() {
        assertThat(filter.filter("🚗 Car Lane")).isEqualTo(" Car Lane")
        assertThat(filter.filter("🏠 Home Street")).isEqualTo(" Home Street")
        assertThat(filter.filter("Near 🏢 Building")).isEqualTo("Near  Building")
    }

    @Test
    fun `filter removes flag emojis`() {
        assertThat(filter.filter("USA 🇺🇸 Street")).isEqualTo("USA  Street")
        assertThat(filter.filter("🇬🇧 London Road")).isEqualTo(" London Road")
    }

    @Test
    fun `filter removes skin tone modifiers`() {
        assertThat(filter.filter("Wave 👋🏻 Street")).isEqualTo("Wave  Street")
        assertThat(filter.filter("Point 👉🏾 Avenue")).isEqualTo("Point  Avenue")
    }

    @Test
    fun `filter removes compound emojis with ZWJ`() {
        // Family emoji (uses Zero Width Joiner)
        assertThat(filter.filter("Family 👨‍👩‍👧 Street")).isEqualTo("Family  Street")
        // Person with red hair
        assertThat(filter.filter("Red 👩‍🦰 Avenue")).isEqualTo("Red  Avenue")
    }

    @Test
    fun `filter handles multiple consecutive emojis`() {
        assertThat(filter.filter("Street 😀😊😎🔥 Name")).isEqualTo("Street  Name")
        assertThat(filter.filter("🏠🚗🏢")).isEqualTo("")
    }

    @Test
    fun `filter handles mixed content`() {
        assertThat(filter.filter("123 😀 Main 😊 Street 😎 Apt 5"))
            .isEqualTo("123  Main  Street  Apt 5")
        assertThat(filter.filter("🏠123🚗Main🏢Street"))
            .isEqualTo("123MainStreet")
    }

    @Test
    fun `filter handles empty string`() {
        assertThat(filter.filter("")).isEqualTo("")
    }

    @Test
    fun `filter handles string with only emojis`() {
        assertThat(filter.filter("😀😊😎")).isEqualTo("")
        assertThat(filter.filter("🏠🚗🏢🔥")).isEqualTo("")
    }

    @Test
    fun `filter preserves special address characters`() {
        assertThat(filter.filter("Suite #100")).isEqualTo("Suite #100")
        assertThat(filter.filter("P.O. Box 123")).isEqualTo("P.O. Box 123")
        assertThat(filter.filter("Unit A-5")).isEqualTo("Unit A-5")
        assertThat(filter.filter("123½ Main St")).isEqualTo("123½ Main St")
        assertThat(filter.filter("Address: 123 & 456")).isEqualTo("Address: 123 & 456")
    }

    @Test
    fun `filter preserves numbers and letters from various languages`() {
        assertThat(filter.filter("Rue de la Paix")).isEqualTo("Rue de la Paix")
        assertThat(filter.filter("Straße 123")).isEqualTo("Straße 123")
        assertThat(filter.filter("Calle España")).isEqualTo("Calle España")
    }

    @Test
    fun `filter handles text with line breaks and special whitespace`() {
        assertThat(filter.filter("123 Main St\nApt 5")).isEqualTo("123 Main St\nApt 5")
        assertThat(filter.filter("Suite  100")).isEqualTo("Suite  100")
        assertThat(filter.filter("Address\t123")).isEqualTo("Address\t123")
    }

    @Test
    fun `filter removes keycap emojis`() {
        assertThat(filter.filter("Number 1️⃣ Street")).isEqualTo("Number  Street")
        assertThat(filter.filter("5️⃣ Fifth Avenue")).isEqualTo(" Fifth Avenue")
    }

    @Test
    fun `filter removes newer emojis from supplemental block`() {
        // Emojis from U+1F900-U+1F9FF range (faces, animals, food, etc.)
        assertThat(filter.filter("Crazy 🤪 Street")).isEqualTo("Crazy  Street")
        assertThat(filter.filter("Brain 🧠 Avenue")).isEqualTo("Brain  Avenue")
        assertThat(filter.filter("Zombie 🧟 Road")).isEqualTo("Zombie  Road")
    }

    // Comprehensive tests to ensure valid addresses are NOT filtered

    @Test
    fun `filter preserves all digits 0-9 in addresses`() {
        assertThat(filter.filter("0 Main Street")).isEqualTo("0 Main Street")
        assertThat(filter.filter("1234567890 Broadway")).isEqualTo("1234567890 Broadway")
        assertThat(filter.filter("Apartment 5")).isEqualTo("Apartment 5")
        assertThat(filter.filter("Unit 9B")).isEqualTo("Unit 9B")
        assertThat(filter.filter("Building 0")).isEqualTo("Building 0")
    }

    @Test
    fun `filter preserves hash symbol not in keycap sequence`() {
        assertThat(filter.filter("Suite #100")).isEqualTo("Suite #100")
        assertThat(filter.filter("#1 Park Avenue")).isEqualTo("#1 Park Avenue")
        assertThat(filter.filter("Apt #5")).isEqualTo("Apt #5")
        assertThat(filter.filter("# 123")).isEqualTo("# 123")
    }

    @Test
    fun `filter preserves common address punctuation`() {
        assertThat(filter.filter("123 Main St.")).isEqualTo("123 Main St.")
        assertThat(filter.filter("P.O. Box 456")).isEqualTo("P.O. Box 456")
        assertThat(filter.filter("Unit A-5")).isEqualTo("Unit A-5")
        assertThat(filter.filter("Apt. 3B")).isEqualTo("Apt. 3B")
        assertThat(filter.filter("123, Main Street")).isEqualTo("123, Main Street")
        assertThat(filter.filter("Suite: 100")).isEqualTo("Suite: 100")
        assertThat(filter.filter("Address; Line 2")).isEqualTo("Address; Line 2")
    }

    @Test
    fun `filter preserves parentheses and brackets`() {
        assertThat(filter.filter("123 Main (rear)")).isEqualTo("123 Main (rear)")
        assertThat(filter.filter("Building [A]")).isEqualTo("Building [A]")
        assertThat(filter.filter("Unit {5}")).isEqualTo("Unit {5}")
        assertThat(filter.filter("(Corner of 5th & Main)")).isEqualTo("(Corner of 5th & Main)")
    }

    @Test
    fun `filter preserves slashes and backslashes`() {
        assertThat(filter.filter("123/456 Main St")).isEqualTo("123/456 Main St")
        assertThat(filter.filter("Unit 5/A")).isEqualTo("Unit 5/A")
        assertThat(filter.filter("C:\\Program Files")).isEqualTo("C:\\Program Files")
        assertThat(filter.filter("123\\456")).isEqualTo("123\\456")
    }

    @Test
    fun `filter preserves mathematical and currency symbols`() {
        assertThat(filter.filter("123 + 456")).isEqualTo("123 + 456")
        assertThat(filter.filter("$100 Street")).isEqualTo("$100 Street")
        assertThat(filter.filter("123 = 456")).isEqualTo("123 = 456")
        assertThat(filter.filter("< 100")).isEqualTo("< 100")
        assertThat(filter.filter("> 50")).isEqualTo("> 50")
        assertThat(filter.filter("€ Zone")).isEqualTo("€ Zone")
        assertThat(filter.filter("£100 Road")).isEqualTo("£100 Road")
        assertThat(filter.filter("¥ Street")).isEqualTo("¥ Street")
    }

    @Test
    fun `filter preserves quotes and apostrophes`() {
        assertThat(filter.filter("O'Brien Street")).isEqualTo("O'Brien Street")
        assertThat(filter.filter("St. Mary's Avenue")).isEqualTo("St. Mary's Avenue")
        assertThat(filter.filter("\"The Plaza\"")).isEqualTo("\"The Plaza\"")
        assertThat(filter.filter("'Main' Street")).isEqualTo("'Main' Street")
    }

    @Test
    fun `filter preserves international characters`() {
        // European characters
        assertThat(filter.filter("Café René")).isEqualTo("Café René")
        assertThat(filter.filter("Zürich Straße")).isEqualTo("Zürich Straße")
        assertThat(filter.filter("Señor José")).isEqualTo("Señor José")
        assertThat(filter.filter("Øvre gate")).isEqualTo("Øvre gate")

        // French
        assertThat(filter.filter("Champs-Élysées")).isEqualTo("Champs-Élysées")
        assertThat(filter.filter("Rue de la Paix")).isEqualTo("Rue de la Paix")

        // German
        assertThat(filter.filter("Goethestraße")).isEqualTo("Goethestraße")
        assertThat(filter.filter("Österreich")).isEqualTo("Österreich")

        // Spanish
        assertThat(filter.filter("Calle España")).isEqualTo("Calle España")
        assertThat(filter.filter("Niño Street")).isEqualTo("Niño Street")

        // Nordic
        assertThat(filter.filter("Åsgatan")).isEqualTo("Åsgatan")
    }

    @Test
    fun `filter preserves common address abbreviations with periods`() {
        assertThat(filter.filter("St. Louis")).isEqualTo("St. Louis")
        assertThat(filter.filter("Ave. de la Paix")).isEqualTo("Ave. de la Paix")
        assertThat(filter.filter("Blvd. Central")).isEqualTo("Blvd. Central")
        assertThat(filter.filter("Dr. Martin Luther King Jr. Blvd")).isEqualTo("Dr. Martin Luther King Jr. Blvd")
        assertThat(filter.filter("Mt. Vernon")).isEqualTo("Mt. Vernon")
    }

    @Test
    fun `filter preserves cardinal and ordinal directions`() {
        assertThat(filter.filter("123 N Main Street")).isEqualTo("123 N Main Street")
        assertThat(filter.filter("456 South Ave")).isEqualTo("456 South Ave")
        assertThat(filter.filter("789 E. 5th St")).isEqualTo("789 E. 5th St")
        assertThat(filter.filter("NW 123rd Street")).isEqualTo("NW 123rd Street")
        assertThat(filter.filter("1st Avenue")).isEqualTo("1st Avenue")
        assertThat(filter.filter("2nd Street")).isEqualTo("2nd Street")
        assertThat(filter.filter("3rd Place")).isEqualTo("3rd Place")
        assertThat(filter.filter("42nd Street")).isEqualTo("42nd Street")
    }

    @Test
    fun `filter preserves complex real-world addresses`() {
        assertThat(filter.filter("1600 Pennsylvania Avenue NW")).isEqualTo("1600 Pennsylvania Avenue NW")
        assertThat(filter.filter("350 5th Ave, New York, NY 10118")).isEqualTo("350 5th Ave, New York, NY 10118")
        assertThat(filter.filter("221B Baker Street")).isEqualTo("221B Baker Street")
        assertThat(filter.filter("10 Downing Street, Westminster, London SW1A 2AA"))
            .isEqualTo("10 Downing Street, Westminster, London SW1A 2AA")
        assertThat(filter.filter("1 Infinite Loop, Cupertino, CA 95014"))
            .isEqualTo("1 Infinite Loop, Cupertino, CA 95014")
    }

    @Test
    fun `filter preserves mixed alphanumeric building codes`() {
        assertThat(filter.filter("Building A1")).isEqualTo("Building A1")
        assertThat(filter.filter("Tower 2B")).isEqualTo("Tower 2B")
        assertThat(filter.filter("Unit 3C5")).isEqualTo("Unit 3C5")
        assertThat(filter.filter("Section Z9")).isEqualTo("Section Z9")
    }

    @Test
    fun `filter preserves percentage and special numeric notations`() {
        assertThat(filter.filter("100% Street")).isEqualTo("100% Street")
        assertThat(filter.filter("123½ Main St")).isEqualTo("123½ Main St")
        assertThat(filter.filter("456¼ Avenue")).isEqualTo("456¼ Avenue")
        assertThat(filter.filter("789¾ Platform")).isEqualTo("789¾ Platform")
    }

    @Test
    fun `filter preserves asterisks and other special characters`() {
        assertThat(filter.filter("Building *A*")).isEqualTo("Building *A*")
        assertThat(filter.filter("Unit ^5")).isEqualTo("Unit ^5")
        assertThat(filter.filter("Section ~B")).isEqualTo("Section ~B")
        assertThat(filter.filter("123 @ Main")).isEqualTo("123 @ Main")
        assertThat(filter.filter("Apt. !5")).isEqualTo("Apt. !5")
    }

    @Test
    fun `filter preserves underscores and other separators`() {
        assertThat(filter.filter("Building_A")).isEqualTo("Building_A")
        assertThat(filter.filter("Unit-5")).isEqualTo("Unit-5")
        assertThat(filter.filter("Section|B")).isEqualTo("Section|B")
    }

    // Tests for non-Latin scripts (CJK, Arabic, Cyrillic, etc.)

    @Test
    fun `filter preserves Chinese characters`() {
        // Simplified Chinese
        assertThat(filter.filter("北京市朝阳区")).isEqualTo("北京市朝阳区")
        assertThat(filter.filter("上海市浦东新区")).isEqualTo("上海市浦东新区")
        assertThat(filter.filter("广州市天河区")).isEqualTo("广州市天河区")
        assertThat(filter.filter("中山路123号")).isEqualTo("中山路123号")

        // Traditional Chinese
        assertThat(filter.filter("台北市信義區")).isEqualTo("台北市信義區")
        assertThat(filter.filter("香港九龍")).isEqualTo("香港九龍")
    }

    @Test
    fun `filter preserves Japanese characters`() {
        // Hiragana
        assertThat(filter.filter("あいうえお")).isEqualTo("あいうえお")
        assertThat(filter.filter("ひらがな")).isEqualTo("ひらがな")

        // Katakana
        assertThat(filter.filter("カタカナ")).isEqualTo("カタカナ")
        assertThat(filter.filter("トウキョウ")).isEqualTo("トウキョウ")

        // Kanji
        assertThat(filter.filter("東京都渋谷区")).isEqualTo("東京都渋谷区")
        assertThat(filter.filter("大阪市中央区")).isEqualTo("大阪市中央区")

        // Mixed
        assertThat(filter.filter("東京都千代田区1-1-1")).isEqualTo("東京都千代田区1-1-1")
        assertThat(filter.filter("渋谷ビル5階")).isEqualTo("渋谷ビル5階")
    }

    @Test
    fun `filter preserves Korean characters`() {
        // Hangul
        assertThat(filter.filter("서울특별시")).isEqualTo("서울특별시")
        assertThat(filter.filter("강남구 테헤란로")).isEqualTo("강남구 테헤란로")
        assertThat(filter.filter("부산광역시")).isEqualTo("부산광역시")
        assertThat(filter.filter("대한민국")).isEqualTo("대한민국")

        // Mixed with numbers
        assertThat(filter.filter("서울시 강남구 123번지")).isEqualTo("서울시 강남구 123번지")
    }

    @Test
    fun `filter preserves Arabic and Hebrew characters`() {
        // Arabic
        assertThat(filter.filter("شارع الملك")).isEqualTo("شارع الملك")
        assertThat(filter.filter("الرياض")).isEqualTo("الرياض")
        assertThat(filter.filter("دبي مارينا")).isEqualTo("دبي مارينا")

        // Hebrew
        assertThat(filter.filter("רחוב הרצל")).isEqualTo("רחוב הרצל")
        assertThat(filter.filter("תל אביב")).isEqualTo("תל אביב")
    }

    @Test
    fun `filter preserves Cyrillic characters`() {
        // Russian
        assertThat(filter.filter("Москва")).isEqualTo("Москва")
        assertThat(filter.filter("Санкт-Петербург")).isEqualTo("Санкт-Петербург")
        assertThat(filter.filter("улица Ленина")).isEqualTo("улица Ленина")
        assertThat(filter.filter("проспект Мира 123")).isEqualTo("проспект Мира 123")

        // Ukrainian
        assertThat(filter.filter("Київ")).isEqualTo("Київ")
        assertThat(filter.filter("вулиця Хрещатик")).isEqualTo("вулиця Хрещатик")
    }

    @Test
    fun `filter preserves Thai characters`() {
        assertThat(filter.filter("กรุงเทพมหานคร")).isEqualTo("กรุงเทพมหานคร")
        assertThat(filter.filter("ถนนสุขุมวิท")).isEqualTo("ถนนสุขุมวิท")
        assertThat(filter.filter("เชียงใหม่")).isEqualTo("เชียงใหม่")
    }

    @Test
    fun `filter preserves Hindi and Devanagari characters`() {
        assertThat(filter.filter("नई दिल्ली")).isEqualTo("नई दिल्ली")
        assertThat(filter.filter("मुंबई")).isEqualTo("मुंबई")
        assertThat(filter.filter("बेंगलुरु")).isEqualTo("बेंगलुरु")
    }

    @Test
    fun `filter preserves Greek characters`() {
        assertThat(filter.filter("Αθήνα")).isEqualTo("Αθήνα")
        assertThat(filter.filter("Θεσσαλονίκη")).isEqualTo("Θεσσαλονίκη")
        assertThat(filter.filter("οδός Ερμού")).isEqualTo("οδός Ερμού")
    }

    @Test
    fun `filter preserves mixed scripts in addresses`() {
        // English + Chinese
        assertThat(filter.filter("123 Main Street 北京")).isEqualTo("123 Main Street 北京")

        // English + Japanese
        assertThat(filter.filter("Tokyo Tower 東京タワー")).isEqualTo("Tokyo Tower 東京タワー")

        // English + Korean
        assertThat(filter.filter("Seoul 서울 123")).isEqualTo("Seoul 서울 123")

        // English + Arabic
        assertThat(filter.filter("Dubai دبي Street")).isEqualTo("Dubai دبي Street")

        // Mixed CJK
        assertThat(filter.filter("東京 서울 北京")).isEqualTo("東京 서울 北京")
    }
}
