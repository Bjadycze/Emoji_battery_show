package com.emojibattery.free

/**
 * Sada emoji od nejnižšího po nejvyšší stav baterie.
 * [levels] se rovnoměrně rozdělí na intervaly 0–100 %.
 */
data class EmojiSet(
    val id: String,
    val nameRes: Int,
    val levels: List<String>,
    val charging: String,
    /** Sada se nevykresluje z emoji, ale kreslí se vektorově (plynulá barva i výplň). */
    val drawn: Boolean = false
) {
    fun forLevel(level: Int): String {
        val l = level.coerceIn(0, 100)
        val idx = (l * levels.size) / 101
        return levels[idx.coerceIn(0, levels.size - 1)]
    }

    fun forState(info: BatteryInfo, useChargingEmoji: Boolean): String =
        if (info.charging && useChargingEmoji) charging else forLevel(info.level)

    /** Náhled sady: pět reprezentativních úrovní. */
    fun preview(): String = listOf(5, 30, 55, 80, 100).joinToString(" ") { forLevel(it) }
}

object EmojiSets {

    val all: List<EmojiSet> = listOf(
        EmojiSet(
            "meter", R.string.set_meter,
            listOf("🔋"), "⚡", drawn = true
        ),
        EmojiSet(
            "cars", R.string.set_cars,
            listOf("🛴", "🛵", "🚗", "🚙", "🏎️"), "⛽"
        ),
        EmojiSet(
            "moon", R.string.set_moon,
            listOf("🌑", "🌒", "🌓", "🌔", "🌕"), "🌞"
        ),
        EmojiSet(
            "face", R.string.set_face,
            listOf("😵", "😟", "😐", "🙂", "😄"), "🤩"
        ),
        EmojiSet(
            "heart", R.string.set_heart,
            listOf("🖤", "💔", "❤️‍🩹", "❤️", "💖"), "💘"
        ),
        EmojiSet(
            "weather", R.string.set_weather,
            listOf("⛈️", "🌧️", "🌥️", "⛅", "☀️"), "🌈"
        ),
        EmojiSet(
            "plant", R.string.set_plant,
            listOf("🥀", "🌱", "🌿", "🌷", "🌳"), "💧"
        ),
        EmojiSet(
            "food", R.string.set_food,
            listOf("🍽️", "🍪", "🍔", "🍕", "🎂"), "🧑‍🍳"
        ),
        EmojiSet(
            "cat", R.string.set_cat,
            listOf("😿", "🙀", "😺", "😸", "😻"), "🐾"
        ),
        EmojiSet(
            "dots", R.string.set_dots,
            listOf("🟥", "🟧", "🟨", "🟩", "🟩"), "🟦"
        )
    )

    fun byId(id: String): EmojiSet = all.firstOrNull { it.id == id } ?: all.first()
}
