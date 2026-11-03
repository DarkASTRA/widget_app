package com.mytextwidget.app.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mytextwidget.app.data.Note
import com.mytextwidget.app.data.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    val allNotes: LiveData<List<Note>> = repository.allNotes.asLiveData()

    fun insert(note: Note, onInserted: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insert(note)
            onInserted(id)
        }
    }

    fun update(note: Note, onUpdated: () -> Unit = {}) {
        viewModelScope.launch {
            repository.update(note)
            onUpdated()
        }
    }

    fun delete(note: Note, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.delete(note)
            onDeleted()
        }
    }

    suspend fun getNoteById(id: Long): Note? = repository.getNoteById(id)
}
