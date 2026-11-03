package com.mytextwidget.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mytextwidget.app.MyTextWidgetApp
import com.mytextwidget.app.R
import com.mytextwidget.app.data.Note
import com.mytextwidget.app.databinding.ActivityMainBinding
import com.mytextwidget.app.widget.NoteWidgetProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotesAdapter

    private val viewModel: NoteViewModel by viewModels {
        NoteViewModelFactory((application as MyTextWidgetApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeNotes()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_help) {
                HelpBottomSheet.newInstance().show(supportFragmentManager, HelpBottomSheet.TAG)
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = NotesAdapter(
            onNoteClicked = { note -> openNoteEditor(note.id) },
            onPinWidgetClicked = { note -> pinWidget(note) },
            onDeleteClicked = { note -> confirmDeleteNote(note) }
        )
        binding.recyclerNotes.layoutManager = LinearLayoutManager(this)
        binding.recyclerNotes.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabNewNote.setOnClickListener { openNoteEditor(-1L) }
        binding.btnEmptyNewNote.setOnClickListener { openNoteEditor(-1L) }
    }

    private fun observeNotes() {
        viewModel.allNotes.observe(this) { list ->
            adapter.submitList(list)
            if (list.isNullOrEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerNotes.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.recyclerNotes.visibility = View.VISIBLE
            }
        }
    }

    private fun openNoteEditor(noteId: Long) {
        val intent = Intent(this, NoteEditActivity::class.java).apply {
            putExtra(NoteEditActivity.EXTRA_NOTE_ID, noteId)
        }
        startActivity(intent)
    }

    private fun pinWidget(note: Note) {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val myProvider = ComponentName(this, NoteWidgetProvider::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
            val callbackIntent = Intent(this, NoteWidgetProvider::class.java).apply {
                action = NoteWidgetProvider.ACTION_WIDGET_PINNED
                putExtra(NoteWidgetProvider.EXTRA_PINNED_NOTE_ID, note.id)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val successCallback = PendingIntent.getBroadcast(this, note.id.toInt(), callbackIntent, flags)
            appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
            Toast.makeText(this, "Placing widget for: ${note.displayTitle()}", Toast.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, R.string.pin_not_supported, Snackbar.LENGTH_LONG)
                .setAction("Help") {
                    HelpBottomSheet.newInstance().show(supportFragmentManager, HelpBottomSheet.TAG)
                }.show()
        }
    }

    private fun confirmDeleteNote(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_note_confirm_title)
            .setMessage(R.string.delete_note_confirm_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.delete(note) {
                    NoteWidgetProvider.updateAllWidgetsForNote(this@MainActivity, note.id)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
