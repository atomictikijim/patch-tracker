package com.prolocity.patchtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Player::class, PatchType::class, PatchAwardEvent::class, PatchAwardLine::class, Team::class, TeamMember::class],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun patchTypeDao(): PatchTypeDao
    abstract fun patchAwardDao(): PatchAwardDao
    abstract fun teamDao(): TeamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "patch_tracker.db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        // Runs on every open (not just first creation) so the default catalog is
                        // restored after a destructive migration, using unique-name IGNORE inserts
                        // so it never duplicates patches the league already has.
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            scope.launch(Dispatchers.IO) {
                                INSTANCE?.patchTypeDao()?.insertAll(
                                    DefaultPatchTypes.SEEDS.map {
                                        PatchType(name = it.name, iconKey = it.iconKey, badgeText = it.badgeText)
                                    }
                                )
                            }
                        }
                    })
                    // Pre-release schema: no real user data to preserve across patch-catalog updates.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
