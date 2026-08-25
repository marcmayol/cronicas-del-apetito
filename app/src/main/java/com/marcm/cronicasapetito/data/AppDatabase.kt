package com.marcm.cronicasapetito.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MealEntry::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN kind TEXT NOT NULL DEFAULT 'food'")
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN minutes INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN photoPath TEXT")
            }
        }

        /**
         * La respuesta del gimnasio dejó de ser texto en español para poder
         * traducir la app sin que los registros viejos dejaran de contar. Se
         * reescriben aquí de una vez; aun así [GymAnswer.esAfirmativo] sigue
         * reconociendo el formato antiguo, por si una base llega sin migrar.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE meal_entries SET content = '${GymAnswer.YES}' " +
                        "WHERE kind = '${EntryKind.GYM}' AND TRIM(content) = '${GymAnswer.LEGACY_SI}'"
                )
                db.execSQL(
                    "UPDATE meal_entries SET content = '${GymAnswer.NO}' " +
                        "WHERE kind = '${EntryKind.GYM}' AND TRIM(content) = 'No'"
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cronicas.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
