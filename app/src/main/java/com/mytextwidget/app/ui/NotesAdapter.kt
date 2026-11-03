package com.mytextwidget.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mytextwidget.app.data.Note
import com.mytextwidget.app.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val onNoteClicked: (Note) -> Unit,
    private val onPinWidgetClicked: (Note) -> Unit,
    private val onDeleteClicked: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback) {

    companion object NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

        fun bind(note: Note) {
            binding.textNoteTitle.text = note.displayTitle()
            val content = if (note.content.isBlank()) "(Empty note)" else note.content
            binding.textNotePreview.text = content
            binding.textNoteDate.text = dateFormat.format(Date(note.updatedAt))

            binding.root.setOnClickListener { onNoteClicked(note) }
            binding.btnPinWidget.setOnClickListener { onPinWidgetClicked(note) }
            binding.btnDelete.setOnClickListener { onDeleteClicked(note) }
        }
    }
}
