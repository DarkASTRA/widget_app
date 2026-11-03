package com.mytextwidget.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.view.GravityCompat
import com.mytextwidget.app.MyTextWidgetApp
import com.mytextwidget.app.R
import com.mytextwidget.app.ui.NoteEditActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetViewsFactory(applicationContext, intent)
    }
}

class NoteWidgetViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0)
    private var config: WidgetConfig = WidgetConfig()
    private var noteId: Long = -1L
    private var isNoteFound: Boolean = false
    private var noteLines: List<String> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        config = WidgetPrefs.getConfig(context, appWidgetId)
        noteId = config.noteId

        if (noteId == -1L) {
            isNoteFound = false
            noteLines = listOf(context.getString(R.string.widget_no_note_selected))
            return
        }

        runBlocking(Dispatchers.IO) {
            try {
                val note = MyTextWidgetApp.instance.repository.getNoteById(noteId)
                if (note != null) {
                    isNoteFound = true
                    val content = note.content
                    val rawLines = if (content.isNotEmpty()) {
                        content.lines()
                    } else if (note.title.isNotEmpty()) {
                        listOf(note.title)
                    } else {
                        listOf(context.getString(R.string.widget_empty_note))
                    }
                    noteLines = rawLines.map { line ->
                        if (line.isEmpty()) " " else line
                    }
                } else {
                    isNoteFound = false
                    noteLines = listOf(context.getString(R.string.widget_note_deleted))
                }
            } catch (e: Exception) {
                isNoteFound = false
                noteLines = listOf(context.getString(R.string.widget_no_note_selected))
            }
        }
    }

    override fun onDestroy() {
        noteLines = emptyList()
    }

    override fun getCount(): Int = noteLines.size

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item_text)

        if (position in noteLines.indices) {
            val text = noteLines[position]
            rv.setTextViewText(R.id.widget_item_line, text)
            rv.setTextViewTextSize(R.id.widget_item_line, android.util.TypedValue.COMPLEX_UNIT_SP, config.getEffectiveTextSizeSp())
            rv.setTextColor(R.id.widget_item_line, config.textColor)

            val gravity = if (config.alignment == WidgetConfig.ALIGN_CENTER) {
                Gravity.CENTER_HORIZONTAL
            } else {
                GravityCompat.START
            }
            rv.setInt(R.id.widget_item_line, "setGravity", gravity)

            val fillInIntent = if (noteId != -1L && isNoteFound) {
                Intent().apply {
                    putExtra(NoteEditActivity.EXTRA_NOTE_ID, noteId)
                }
            } else {
                Intent(context, WidgetConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
            }
            rv.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)
        }

        return rv
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_item_text).apply {
            setTextViewText(R.id.widget_item_line, "Loading…")
            setTextColor(R.id.widget_item_line, config.textColor)
        }
    }

    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
