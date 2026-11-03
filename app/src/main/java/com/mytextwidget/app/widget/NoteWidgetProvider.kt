package com.mytextwidget.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.mytextwidget.app.MyTextWidgetApp
import com.mytextwidget.app.R
import com.mytextwidget.app.data.Note
import com.mytextwidget.app.ui.NoteEditActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class NoteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_ALL_WIDGETS = "com.mytextwidget.app.ACTION_REFRESH_ALL_WIDGETS"
        const val ACTION_WIDGET_PINNED = "com.mytextwidget.app.ACTION_WIDGET_PINNED"
        const val EXTRA_PINNED_NOTE_ID = "extra_pinned_note_id"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val config = WidgetPrefs.getConfig(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_note)

            views.setInt(R.id.widget_root, "setBackgroundColor", config.getResolvedBackgroundColor())

            val note: Note? = if (config.noteId != -1L) {
                runBlocking(Dispatchers.IO) {
                    try {
                        MyTextWidgetApp.instance.repository.getNoteById(config.noteId)
                    } catch (e: Exception) {
                        null
                    }
                }
            } else {
                null
            }

            val editIntent = if (config.noteId != -1L && note != null) {
                Intent(context, NoteEditActivity::class.java).apply {
                    putExtra(NoteEditActivity.EXTRA_NOTE_ID, config.noteId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            } else {
                Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
            val editPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                editIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_edit, editPendingIntent)

            if (config.noteId == -1L || note == null) {
                val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 1000 + 1,
                    configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, configPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_empty_view, configPendingIntent)
            }

            val serviceIntent = Intent(context, NoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("content://com.mytextwidget.app/widget/$appWidgetId")
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            val emptyMessage = when {
                config.noteId == -1L -> context.getString(R.string.widget_no_note_selected)
                note == null -> context.getString(R.string.widget_note_deleted)
                note.content.isBlank() && note.title.isBlank() -> context.getString(R.string.widget_empty_note)
                else -> ""
            }
            views.setTextViewText(R.id.widget_empty_view, emptyMessage)
            views.setTextColor(R.id.widget_empty_view, config.textColor)

            val itemClickIntent = Intent(context, NoteEditActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val itemPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 1000 + 2,
                itemClickIntent,
                flags
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, itemPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NoteWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component) ?: return
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun updateAllWidgetsForNote(context: Context, noteId: Long) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NoteWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component) ?: return
            for (id in appWidgetIds) {
                val widgetNoteId = WidgetPrefs.getNoteIdForWidget(context, id)
                if (widgetNoteId == noteId || widgetNoteId == -1L) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            WidgetPrefs.deleteConfig(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_PINNED -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
                val noteId = intent.getLongExtra(EXTRA_PINNED_NOTE_ID, -1L)
                if (appWidgetId != 0 && noteId != -1L) {
                    val config = WidgetPrefs.getConfig(context, appWidgetId).copy(noteId = noteId)
                    WidgetPrefs.saveConfig(context, appWidgetId, config)
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                } else {
                    updateAllWidgets(context)
                }
            }
            ACTION_REFRESH_ALL_WIDGETS, Intent.ACTION_BOOT_COMPLETED -> {
                updateAllWidgets(context)
            }
        }
    }
}
