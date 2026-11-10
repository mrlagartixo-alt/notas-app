package com.notas.app.util

import android.content.Context
import android.content.Intent
import com.notas.app.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(ms: Long): String =
    SimpleDateFormat("d MMM · HH:mm", Locale("pt", "BR")).format(Date(ms))

fun noteToText(note: Note): String =
    listOf(note.title.trim(), note.content.trim())
        .filter { it.isNotEmpty() }
        .joinToString("\n\n")

fun notesToText(notes: List<Note>): String =
    notes.joinToString("\n\n————————————\n\n") { noteToText(it) }

fun shareText(context: Context, subject: String?, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, "Compartilhar").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
