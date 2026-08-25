package com.marcm.cronicasapetito.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(entities = [MealEntry::class], version = AppDatabase.VERSION_ACTUAL, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealEntryDao

    companion object {
        const val VERSION_ACTUAL = 4
        const val NOMBRE = "cronicas.db"
        private const val TAG = "AppDatabase"

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
                INSTANCE ?: run {
                    copiaAntesDeMigrar(context)
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        NOMBRE
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }

        /**
         * Antes de que Room abra una base más antigua que la actual, guarda el
         * archivo tal cual está. Una migración no debería perder nada —esta se
         * probó actualizando encima de la versión anterior— pero el historial de
         * meses de alguien no es sitio para confiar en «no debería»: si algo
         * saliera mal, el original sigue en la carpeta de la app y se puede
         * volver a poner.
         *
         * Solo se guarda una copia por versión de origen, así que abrir la app
         * cien veces no llena el disco de copias.
         */
        private fun copiaAntesDeMigrar(context: Context) {
            runCatching {
                val base = context.getDatabasePath(NOMBRE)
                if (!base.exists()) return
                val version = versionDe(base) ?: return
                if (version >= VERSION_ACTUAL) return

                val copia = File(base.parentFile, "$NOMBRE.antes-de-v$VERSION_ACTUAL")
                if (copia.exists()) return
                base.copyTo(copia, overwrite = false)
                Log.i(TAG, "copia previa a la migración en ${copia.name}")
            }.onFailure {
                // Si la copia falla no se bloquea la app: se registra y se sigue.
                Log.e(TAG, "no se pudo copiar la base antes de migrar", it)
            }
        }

        /** Lee `user_version` del archivo SQLite sin abrirlo con Room. */
        private fun versionDe(archivo: File): Int? = runCatching {
            SQLiteDatabase.openDatabase(
                archivo.path, null, SQLiteDatabase.OPEN_READONLY
            ).use { it.version }
        }.getOrNull()
    }
}
