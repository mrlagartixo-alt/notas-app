package com.notas.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Category::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "anota.db")
                .addCallback(object : RoomDatabase.Callback() {
                    // Roda só uma vez, quando o banco é criado: categorias iniciais
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "INSERT INTO categories (name, colorIndex) VALUES " +
                                "('Pessoal', 0), ('Trabalho', 4), ('Ideias', 1), ('Compras', 2)"
                        )
                    }
                })
                .build()
                .also { instance = it }
        }
    }
}
