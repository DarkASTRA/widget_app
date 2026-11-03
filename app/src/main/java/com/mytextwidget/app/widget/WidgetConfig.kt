package com.mytextwidget.app.widget

data class WidgetConfig(
    val noteId: Long = -1L,
    val textSizeSp: Float = 16.0f,
    val textColor: Int = -1,
    val bgColor: Int = -15198181,
    val bgAlphaPercent: Int = 90,
    val alignment: Int = ALIGN_LEFT,
    val showTitle: Boolean = false,
    val fitText: Boolean = false
) {
    companion object {
        const val ALIGN_LEFT = 0
        const val ALIGN_CENTER = 1

        val PRESET_DARK = WidgetConfig(
            noteId = -1L,
            textSizeSp = 16.0f,
            textColor = -1,
            bgColor = -15198181,
            bgAlphaPercent = 92,
            alignment = ALIGN_LEFT,
            showTitle = false,
            fitText = false
        )

        val PRESET_LIGHT = WidgetConfig(
            noteId = -1L,
            textSizeSp = 16.0f,
            textColor = -15198181,
            bgColor = -1,
            bgAlphaPercent = 95,
            alignment = ALIGN_LEFT,
            showTitle = false,
            fitText = false
        )

        val PRESET_OLED = WidgetConfig(
            noteId = -1L,
            textSizeSp = 16.0f,
            textColor = -1,
            bgColor = -16777216,
            bgAlphaPercent = 100,
            alignment = ALIGN_LEFT,
            showTitle = false,
            fitText = false
        )

        val PRESET_WARM = WidgetConfig(
            noteId = -1L,
            textSizeSp = 16.0f,
            textColor = -13819111,
            bgColor = -266023,
            bgAlphaPercent = 95,
            alignment = ALIGN_LEFT,
            showTitle = false,
            fitText = false
        )

        val PRESET_TRANSLUCENT = WidgetConfig(
            noteId = -1L,
            textSizeSp = 16.0f,
            textColor = -1,
            bgColor = -14800581,
            bgAlphaPercent = 40,
            alignment = ALIGN_LEFT,
            showTitle = false,
            fitText = false
        )

        val PRESET_NAVY = WidgetConfig(
            noteId = -1L,
            textSizeSp = 16.0f,
            textColor = -1906448,
            bgColor = -15788246,
            bgAlphaPercent = 92,
            alignment = ALIGN_LEFT,
            showTitle = false,
            fitText = false
        )
    }

    fun getResolvedBackgroundColor(): Int {
        val clampedAlpha = bgAlphaPercent.coerceIn(0, 100)
        val alpha = (255 * (clampedAlpha / 100.0f)).toInt()
        val red = (bgColor shr 16) and 255
        val green = (bgColor shr 8) and 255
        val blue = bgColor and 255
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    fun getEffectiveTextSizeSp(): Float {
        return if (fitText) {
            (textSizeSp * 0.85f).coerceAtLeast(12.0f)
        } else {
            textSizeSp
        }
    }
}
