package com.mytextwidget.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.mytextwidget.app.MyTextWidgetApp
import com.mytextwidget.app.R
import com.mytextwidget.app.data.Note
import com.mytextwidget.app.databinding.ActivityWidgetConfigBinding
import com.mytextwidget.app.ui.NoteEditActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRESELECTED_NOTE_ID = "extra_preselected_note_id"
    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: ActivityWidgetConfigBinding
    private var selectedNote: Note? = null
    private var allNotes: List<Note> = emptyList()
    private var currentConfig: WidgetConfig = WidgetConfig()

    private val writeTextLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        loadNotesAndInitialize(-1L)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val passedNoteId = intent.getLongExtra(EXTRA_PRESELECTED_NOTE_ID, -1L)
        currentConfig = WidgetPrefs.getConfig(this, appWidgetId)
        if (passedNoteId != -1L) {
            currentConfig = currentConfig.copy(noteId = passedNoteId)
        }

        setupUI()
        loadNotesAndInitialize(currentConfig.noteId)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentConfig.noteId != -1L) {
                    applyAndSaveWidget()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (allNotes.isNotEmpty()) {
            loadNotesAndInitialize(currentConfig.noteId)
        }
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            if (currentConfig.noteId != -1L) {
                applyAndSaveWidget()
            } else {
                finish()
            }
        }

        binding.cardSelectNote.setOnClickListener {
            showNoteSelectionDialog()
        }

        binding.btnApplyWidget.setOnClickListener {
            applyAndSaveWidget()
        }

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnWriteText.setOnClickListener { showNoteSelectionDialog() }
        binding.toggleTextSize.check(textSizeButtonId())
        binding.toggleTextSize.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val size = when (checkedId) {
                    R.id.btn_size_small -> 12f
                    R.id.btn_size_large -> 22f
                    R.id.btn_size_xlarge -> 28f
                    else -> 16f
                }
                currentConfig = currentConfig.copy(textSizeSp = size)
                updatePreview()
            }
        }
        binding.sliderOpacity.value = currentConfig.bgAlphaPercent.toFloat().coerceIn(0f, 100f)
        binding.sliderOpacity.addOnChangeListener { _, value, _ ->
            currentConfig = currentConfig.copy(bgAlphaPercent = value.toInt())
            updatePreview()
        }

        binding.toggleAlignment.check(
            if (currentConfig.alignment == WidgetConfig.ALIGN_CENTER) R.id.btn_align_center else R.id.btn_align_left
        )
        binding.toggleAlignment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val align = if (checkedId == R.id.btn_align_center) WidgetConfig.ALIGN_CENTER else WidgetConfig.ALIGN_LEFT
                currentConfig = currentConfig.copy(alignment = align)
                updatePreview()
            }
        }

        binding.switchFitText.isChecked = currentConfig.fitText
        binding.switchFitText.setOnCheckedChangeListener { _, isChecked ->
            currentConfig = currentConfig.copy(fitText = isChecked)
            updatePreview()
        }

        binding.chipDark.setOnClickListener { applyPreset(WidgetConfig.PRESET_DARK) }
        binding.chipLight.setOnClickListener { applyPreset(WidgetConfig.PRESET_LIGHT) }
        binding.chipOled.setOnClickListener { applyPreset(WidgetConfig.PRESET_OLED) }
        binding.chipWarm.setOnClickListener { applyPreset(WidgetConfig.PRESET_WARM) }
        binding.chipTransparent.setOnClickListener { applyPreset(WidgetConfig.PRESET_TRANSLUCENT) }
        binding.chipNavy.setOnClickListener { applyPreset(WidgetConfig.PRESET_NAVY) }
    }

    private fun applyPreset(preset: WidgetConfig) {
        currentConfig = currentConfig.copy(
            textColor = preset.textColor,
            bgColor = preset.bgColor,
            bgAlphaPercent = preset.bgAlphaPercent,
            alignment = preset.alignment,
            showTitle = false,
            fitText = preset.fitText
        )
        syncControlsWithConfig()
        updatePreview()
    }

    private fun syncControlsWithConfig() {
        binding.toggleTextSize.check(textSizeButtonId())
        binding.sliderOpacity.value = currentConfig.bgAlphaPercent.toFloat().coerceIn(0f, 100f)
        binding.switchFitText.isChecked = currentConfig.fitText
        binding.toggleAlignment.check(
            if (currentConfig.alignment == WidgetConfig.ALIGN_CENTER) R.id.btn_align_center else R.id.btn_align_left
        )
    }

    private fun loadNotesAndInitialize(preselectedNoteId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = (application as MyTextWidgetApp).repository
            val notes = repository.getAllNotesList()
            allNotes = notes

            withContext(Dispatchers.Main) {
                if (notes.isEmpty()) {
                    selectedNote = null
                    currentConfig = currentConfig.copy(noteId = -1L)
                    binding.textSelectedNoteName.text = getString(R.string.no_notes_title)

                    updatePreview()

                    val intent = Intent(this@WidgetConfigActivity, NoteEditActivity::class.java).apply {
                        putExtra(NoteEditActivity.EXTRA_LINK_TO_WIDGET_ID, appWidgetId)
                    }
                    writeTextLauncher.launch(intent)
                    return@withContext
                }

                val noteToSelect = when {
                    preselectedNoteId != -1L -> notes.find { it.id == preselectedNoteId } ?: notes.first()
                    currentConfig.noteId != -1L -> notes.find { it.id == currentConfig.noteId } ?: notes.first()
                    notes.size == 1 -> notes.first()
                    else -> notes.first()
                }

                selectedNote = noteToSelect
                currentConfig = currentConfig.copy(noteId = noteToSelect.id)
                binding.textSelectedNoteName.text = noteToSelect.displayTitle()

                updatePreview()
            }
        }
    }

    private fun showNoteSelectionDialog() {
        if (allNotes.isEmpty()) {
            val intent = Intent(this, NoteEditActivity::class.java).apply {
                putExtra(NoteEditActivity.EXTRA_LINK_TO_WIDGET_ID, appWidgetId)
            }
            writeTextLauncher.launch(intent)
            return
        }

        val items = mutableListOf<String>()
        items.add("✏️ Write new note")
        items.addAll(allNotes.map { it.displayTitle() })

        val checkedIndex = if (selectedNote != null) {
            val idx = allNotes.indexOfFirst { it.id == selectedNote?.id }
            if (idx != -1) idx + 1 else 0
        } else {
            0
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.select_note)
            .setSingleChoiceItems(items.toTypedArray(), checkedIndex) { dialog, which ->
                if (which == 0) {
                    dialog.dismiss()
                    val intent = Intent(this, NoteEditActivity::class.java).apply {
                        putExtra(NoteEditActivity.EXTRA_LINK_TO_WIDGET_ID, appWidgetId)
                    }
                    writeTextLauncher.launch(intent)
                } else {
                    val note = allNotes[which - 1]
                    selectedNote = note
                    currentConfig = currentConfig.copy(noteId = note.id)
                    binding.textSelectedNoteName.text = note.displayTitle()

                    updatePreview()
                    dialog.dismiss()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun textSizeButtonId(): Int = when {
        currentConfig.textSizeSp <= 12f -> R.id.btn_size_small
        currentConfig.textSizeSp <= 16f -> R.id.btn_size_medium
        currentConfig.textSizeSp <= 22f -> R.id.btn_size_large
        else -> R.id.btn_size_xlarge
    }

    private fun updatePreview() {
        binding.previewWidgetBox.setBackgroundColor(currentConfig.getResolvedBackgroundColor())
        binding.previewText.setTextColor(currentConfig.textColor)
        binding.previewText.textSize = currentConfig.getEffectiveTextSizeSp()
        binding.previewText.gravity = if (currentConfig.alignment == WidgetConfig.ALIGN_CENTER) {
            Gravity.CENTER_HORIZONTAL
        } else {
            GravityCompat.START
        }
        binding.previewText.text = selectedNote?.let { note ->
            note.content.ifBlank { note.displayTitle() }
        } ?: getString(R.string.widget_no_note_selected)
        binding.textOpacityValue.text = getString(R.string.opacity_value, currentConfig.bgAlphaPercent)
    }
    private fun applyAndSaveWidget() {
        if (currentConfig.noteId == -1L && allNotes.isNotEmpty()) {
            currentConfig = currentConfig.copy(noteId = allNotes.first().id)
        }

        WidgetPrefs.saveConfig(this, appWidgetId, currentConfig)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        NoteWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        Toast.makeText(this, "Widget added to home screen", Toast.LENGTH_SHORT).show()
        finish()
    }
}
