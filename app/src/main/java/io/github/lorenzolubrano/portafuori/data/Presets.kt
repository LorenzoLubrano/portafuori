package io.github.lorenzolubrano.portafuori.data

/** Common Italian bins. Colours differ between comuni, so they stay editable. */
data class BinPreset(val key: String, val name: String, val color: Long, val icon: String)

object Presets {
    val bins = listOf(
        BinPreset("organico", "Organico", 0xFF8D5B3A, "compost"),
        BinPreset("plastica", "Plastica e metalli", 0xFFF2C230, "bottle"),
        BinPreset("carta", "Carta e cartone", 0xFF2F6FD6, "paper"),
        BinPreset("vetro", "Vetro", 0xFF2E9E5B, "glass"),
        BinPreset("secco", "Indifferenziato", 0xFF6B7280, "trash"),
        BinPreset("multi", "Multimateriale", 0xFF1FA2C7, "recycle"),
        BinPreset("verde", "Sfalci e potature", 0xFF6E8B3D, "grass"),
        BinPreset("pannolini", "Pannolini e pannoloni", 0xFF8E6CC1, "baby"),
    )

    /** Palette offered in the bin editor. */
    val colors = listOf(
        0xFF8D5B3A, 0xFFF2C230, 0xFF2F6FD6, 0xFF2E9E5B, 0xFF6B7280, 0xFF1FA2C7,
        0xFF6E8B3D, 0xFF8E6CC1, 0xFFD64545, 0xFFE67E22, 0xFF222222, 0xFFB0B7C3,
    )

    val icons = listOf("compost", "bottle", "paper", "glass", "trash", "recycle", "grass", "baby", "bag", "box", "battery", "oil")

    fun byKey(key: String?) = bins.firstOrNull { it.key == key }
}
