package com.mytextwidget.app

import android.app.Application
import com.mytextwidget.app.data.AppDatabase
import com.mytextwidget.app.data.NoteRepository

class MyTextWidgetApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: NoteRepository by lazy { NoteRepository(database.noteDao()) }

    companion object {
        lateinit var instance: MyTextWidgetApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
