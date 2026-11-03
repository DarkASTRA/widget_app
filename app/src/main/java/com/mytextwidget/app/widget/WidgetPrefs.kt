package com.mytextwidget.app.widget

import android.content.Context
import android.content.SharedPreferences

object WidgetPrefs {
    private const val PREFS_NAME = "com.mytextwidget.app.widget_prefs"
    private const val KEY_PREFIX_NOTE_ID = "widget_note_id_"
    private const val KEY_PREFIX_TEXT_SIZE = "widget_text_size_"
    private const val KEY_PREFIX_TEXT_COLOR = "widget_text_color_"
    private const val KEY_PREFIX_BG_COLOR = "widget_bg_color_"
    private const val KEY_PREFIX_BG_ALPHA = "widget_bg_alpha_"
    private const val KEY_PREFIX_ALIGNMENT = "widget_alignment_"
    private const val KEY_PREFIX_SHOW_TITLE = "widget_show_title_"
    private const val KEY_PREFIX_FIT_TEXT = "widget_fit_text_"

    fun saveConfig(context: Context, appWidgetId: Int, config: WidgetConfig): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.edit().apply {
            putLong(KEY_PREFIX_NOTE_ID + appWidgetId, config.noteId)
            putFloat(KEY_PREFIX_TEXT_SIZE + appWidgetId, config.textSizeSp)
            putInt(KEY_PREFIX_TEXT_COLOR + appWidgetId, config.textColor)
            putInt(KEY_PREFIX_BG_COLOR + appWidgetId, config.bgColor)
            putInt(KEY_PREFIX_BG_ALPHA + appWidgetId, config.bgAlphaPercent)
            putInt(KEY_PREFIX_ALIGNMENT + appWidgetId, config.alignment)
            putBoolean(KEY_PREFIX_SHOW_TITLE + appWidgetId, false)
            putBoolean(KEY_PREFIX_FIT_TEXT + appWidgetId, config.fitText)
        }.commit()
    }

    fun getConfig(context: Context, appWidgetId: Int): WidgetConfig {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val noteId = prefs.getLong(KEY_PREFIX_NOTE_ID + appWidgetId, -1L)
        val textSizeSp = prefs.getFloat(KEY_PREFIX_TEXT_SIZE + appWidgetId, 16.0f)
        val textColor = prefs.getInt(KEY_PREFIX_TEXT_COLOR + appWidgetId, -1)
        val bgColor = prefs.getInt(KEY_PREFIX_BG_COLOR + appWidgetId, -15198181)
        val bgAlpha = prefs.getInt(KEY_PREFIX_BG_ALPHA + appWidgetId, 92)
        val alignment = prefs.getInt(KEY_PREFIX_ALIGNMENT + appWidgetId, 0)
        val fitText = prefs.getBoolean(KEY_PREFIX_FIT_TEXT + appWidgetId, false)
        return WidgetConfig(
            noteId = noteId,
            textSizeSp = textSizeSp,
            textColor = textColor,
            bgColor = bgColor,
            bgAlphaPercent = bgAlpha,
            alignment = alignment,
            showTitle = false,
            fitText = fitText
        )
    }

    fun deleteConfig(context: Context, appWidgetId: Int): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.edit().apply {
            remove(KEY_PREFIX_NOTE_ID + appWidgetId)
            remove(KEY_PREFIX_TEXT_SIZE + appWidgetId)
            remove(KEY_PREFIX_TEXT_COLOR + appWidgetId)
            remove(KEY_PREFIX_BG_COLOR + appWidgetId)
            remove(KEY_PREFIX_BG_ALPHA + appWidgetId)
            remove(KEY_PREFIX_ALIGNMENT + appWidgetId)
            remove(KEY_PREFIX_SHOW_TITLE + appWidgetId)
            remove(KEY_PREFIX_FIT_TEXT + appWidgetId)
        }.commit()
    }

    fun getNoteIdForWidget(context: Context, appWidgetId: Int): Long {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_PREFIX_NOTE_ID + appWidgetId, -1L)
    }
}
