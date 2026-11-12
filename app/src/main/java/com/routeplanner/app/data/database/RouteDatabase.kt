package com.routeplanner.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import com.routeplanner.app.data.database.dao.DestinationDao
import com.routeplanner.app.data.database.dao.RouteDao
import com.routeplanner.app.data.database.dao.RouteStopDao

import com.routeplanner.app.data.database.entity.DestinationEntity
import com.routeplanner.app.data.database.entity.RouteEntity
import com.routeplanner.app.data.database.entity.RouteStopEntity

@Database(
    entities = [
        DestinationEntity::class,
        RouteEntity::class,
        RouteStopEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class RouteDatabase : RoomDatabase() {

    abstract fun destinationDao(): DestinationDao

    abstract fun routeDao(): RouteDao

    abstract fun routeStopDao(): RouteStopDao

    companion object {

        @Volatile
        private var INSTANCE: RouteDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS routes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        originAddress TEXT NOT NULL,
                        originLatitude REAL NOT NULL,
                        originLongitude REAL NOT NULL,
                        status TEXT NOT NULL,
                        optimizationType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS route_stops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        routeId INTEGER NOT NULL,
                        destinationId INTEGER,
                        addressSnapshot TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        stopOrder INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        completedAt INTEGER,
                        FOREIGN KEY(routeId)
                            REFERENCES routes(id)
                            ON DELETE CASCADE
                    )
                """)

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_route_stops_routeId
                    ON route_stops(routeId)
                """)

                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_route_stops_destinationId
                    ON route_stops(destinationId)
                """)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
            ALTER TABLE routes
            ADD COLUMN returnToOrigin INTEGER NOT NULL DEFAULT 0
        """)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            ALTER TABLE routes
            ADD COLUMN returnToOriginCompleted INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): RouteDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RouteDatabase::class.java,
                    "route_planner_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4)
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}