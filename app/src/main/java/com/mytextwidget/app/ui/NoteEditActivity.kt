package com.mytextwidget.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mytextwidget.app.MyTextWidgetApp
import com.mytextwidget.app.R
import com.mytextwidget.app.data.Note
import com.mytextwidget.app.data.NoteRepository
import com.mytextwidget.app.databinding.ActivityNoteEditBinding
import com.mytextwidget.app.widget.NoteWidgetProvider
import com.mytextwidget.app.widget.WidgetPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_LINK_TO_WIDGET_ID = "extra_link_to_widget_id"
    }

    private lateinit var binding: ActivityNoteEditBinding
    private val repository: NoteRepository by lazy {
        (application as MyTextWidgetApp).repository
    }

    private var currentNoteId: Long = -1L
    private var linkToWidgetId: Int = 0
    private var initialTitle: String = ""
    private var initialContent: String = ""
    private var isDeleted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        currentNoteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        linkToWidgetId = intent.getIntExtra(EXTRA_LINK_TO_WIDGET_ID, 0)

        setupUI()
        loadNoteIfEditing()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveNoteIfDirty()
                finish()
            }
        })
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            saveNoteIfDirty()
            finish()
        }

        binding.editContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateTextCounts(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        updateTextCounts("")
    }

    private fun updateTextCounts(text: String) {
        val charCount = text.length
        val wordCount = if (text.isNotBlank()) {
            text.trim().split(Regex("\\s+")).size
        } else {
            0
        }
        binding.textCharCount.text = "$wordCount words • $charCount characters"
    }

    private fun loadNoteIfEditing() {
        if (currentNoteId != -1L) {
            lifecycleScope.launch {
                val note = repository.getNoteById(currentNoteId)
                if (note != null) {
                    initialTitle = note.title
                    initialContent = note.content
                    binding.editTitle.setText(note.title)
                    binding.editContent.setText(note.content)
                    supportActionBar?.title = getString(R.string.edit_note)
                    updateTextCounts(note.content)
                }
            }
        } else {
            supportActionBar?.title = getString(R.string.new_note)
            binding.textStatus.text = getString(R.string.draft_saved)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_note_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveNoteExplicitly()
                true
            }
            R.id.action_delete -> {
                confirmDeleteNote()
                true
            }
            R.id.action_pin_widget -> {
                pinWidgetForThisNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!isDeleted) {
            saveNoteIfDirty()
        }
    }

    private fun isDirty(): Boolean {
        val currentTitle = binding.editTitle.text.toString().trim()
        val currentContent = binding.editContent.text.toString()
        return currentTitle != initialTitle || currentContent != initialContent
    }

    private fun saveNoteIfDirty() {
        if (!isDirty() || isDeleted) return

        val title = binding.editTitle.text.toString().trim()
        val content = binding.editContent.text.toString()

        if (title.isNotBlank() || content.isNotBlank() || currentNoteId != -1L) {
            lifecycleScope.launch(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val note = Note(
                    id = if (currentNoteId != -1L) currentNoteId else 0,
                    title = title,
                    content = content,
                    updatedAt = now
                )
                val savedId = if (currentNoteId != -1L) {
                    repository.update(note)
                    currentNoteId
                } else {
                    repository.insert(note)
                }
                currentNoteId = savedId
                initialTitle = title
                initialContent = content

                if (linkToWidgetId != 0) {
                    val config = WidgetPrefs.getConfig(this@NoteEditActivity, linkToWidgetId).copy(noteId = savedId)
                    WidgetPrefs.saveConfig(this@NoteEditActivity, linkToWidgetId, config)
                    val manager = AppWidgetManager.getInstance(this@NoteEditActivity)
                    NoteWidgetProvider.updateAppWidget(this@NoteEditActivity, manager, linkToWidgetId)
                }

                NoteWidgetProvider.updateAllWidgetsForNote(this@NoteEditActivity, savedId)
            }
        }
    }

    private fun saveNoteExplicitly() {
        val title = binding.editTitle.text.toString().trim()
        val content = binding.editContent.text.toString()

        if (title.isNotBlank() || content.isNotBlank()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val note = Note(
                    id = if (currentNoteId != -1L) currentNoteId else 0,
                    title = title,
                    content = content,
                    updatedAt = now
                )
                val savedId = if (currentNoteId != -1L) {
                    repository.update(note)
                    currentNoteId
                } else {
                    repository.insert(note)
                }
                currentNoteId = savedId
                initialTitle = title
                initialContent = content

                if (linkToWidgetId != 0) {
                    val config = WidgetPrefs.getConfig(this@NoteEditActivity, linkToWidgetId).copy(noteId = savedId)
                    WidgetPrefs.saveConfig(this@NoteEditActivity, linkToWidgetId, config)
                    val manager = AppWidgetManager.getInstance(this@NoteEditActivity)
                    NoteWidgetProvider.updateAppWidget(this@NoteEditActivity, manager, linkToWidgetId)
                }

                NoteWidgetProvider.updateAllWidgetsForNote(this@NoteEditActivity, savedId)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NoteEditActivity, R.string.saved, Toast.LENGTH_SHORT).show()
                    binding.textStatus.text = getString(R.string.saved)
                }
            }
        } else {
            Toast.makeText(this, "Note is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteNote() {
        if (currentNoteId == -1L) {
            isDeleted = true
            finish()
        } else {
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_note_confirm_title)
                .setMessage(R.string.delete_note_confirm_message)
                .setPositiveButton(R.string.delete) { _, _ ->
                    deleteNote()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun deleteNote() {
        isDeleted = true
        lifecycleScope.launch(Dispatchers.IO) {
            repository.deleteById(currentNoteId)
            NoteWidgetProvider.updateAllWidgetsForNote(this@NoteEditActivity, currentNoteId)
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }

    private fun pinWidgetForThisNote() {
        val title = binding.editTitle.text.toString().trim()
        val content = binding.editContent.text.toString()

        if (title.isNotBlank() || content.isNotBlank()) {
            lifecycleScope.launch {
                val note = Note(
                    id = if (currentNoteId != -1L) currentNoteId else 0,
                    title = title,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
                val savedId = withContext(Dispatchers.IO) {
                    if (currentNoteId != -1L) {
                        repository.update(note)
                        currentNoteId
                    } else {
                        repository.insert(note)
                    }
                }
                currentNoteId = savedId
                initialTitle = title
                initialContent = content

                val appWidgetManager = AppWidgetManager.getInstance(this@NoteEditActivity)
                val myProvider = ComponentName(this@NoteEditActivity, NoteWidgetProvider::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                    val callbackIntent = Intent(this@NoteEditActivity, NoteWidgetProvider::class.java).apply {
                        action = NoteWidgetProvider.ACTION_WIDGET_PINNED
                        putExtra(NoteWidgetProvider.EXTRA_PINNED_NOTE_ID, savedId)
                    }
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val successCallback = PendingIntent.getBroadcast(this@NoteEditActivity, savedId.toInt(), callbackIntent, flags)
                    appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                    Toast.makeText(this@NoteEditActivity, "Placing widget...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@NoteEditActivity, R.string.pin_not_supported, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Write some content before adding to home screen", Toast.LENGTH_SHORT).show()
        }
    }
}
