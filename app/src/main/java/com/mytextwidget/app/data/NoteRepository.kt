package com.mytextwidget.app.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getAllNotesList(): List<Note> = noteDao.getAllNotesList()

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    fun getNoteByIdFlow(id: Long): Flow<Note?> = noteDao.getNoteByIdFlow(id)

    suspend fun insert(note: Note): Long = noteDao.insert(note)

    suspend fun update(note: Note) = noteDao.update(note)

    suspend fun delete(note: Note) = noteDao.delete(note)

    suspend fun deleteById(id: Long) = noteDao.deleteById(id)
}
