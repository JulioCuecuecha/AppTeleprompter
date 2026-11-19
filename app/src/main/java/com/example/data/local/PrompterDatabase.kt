package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.FolderEntity
import com.example.data.model.ScriptEntity

@Database(
    entities = [ScriptEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PrompterDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    companion object {
        @Volatile
        private var INSTANCE: PrompterDatabase? = null

        fun getInstance(context: Context): PrompterDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PrompterDatabase::class.java,
                    "stageprompt_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
