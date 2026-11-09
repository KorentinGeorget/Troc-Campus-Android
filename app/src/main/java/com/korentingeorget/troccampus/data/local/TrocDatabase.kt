package com.korentingeorget.troccampus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE annonces ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE annonces ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE annonces ADD COLUMN lat REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE annonces ADD COLUMN lng REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE annonces ADD COLUMN accuracy REAL DEFAULT NULL")
        db.execSQL("ALTER TABLE annonces ADD COLUMN photoPath TEXT DEFAULT NULL")
    }
}

@Database(
    entities = [AnnonceEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class TrocDatabase : RoomDatabase() {
    abstract fun annonceDao(): AnnonceDao

    companion object {
        fun build(context: Context): TrocDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                TrocDatabase::class.java,
                "troccampus.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
