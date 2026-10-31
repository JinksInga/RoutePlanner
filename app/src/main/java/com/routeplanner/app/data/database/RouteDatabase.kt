package com.routeplanner.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.routeplanner.app.data.database.dao.DestinationDao
import com.routeplanner.app.data.database.entity.DestinationEntity

@Database(
    entities = [DestinationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RouteDatabase : RoomDatabase() {

    abstract fun destinationDao(): DestinationDao

    companion object {

        @Volatile
        private var INSTANCE: RouteDatabase? = null

        fun getDatabase(context: Context): RouteDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RouteDatabase::class.java,
                    "route_planner_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}
/*
* Aquí estamos utilizando el patrón Singleton para evitar crear varias instancias
* de la misma base de datos.
* El archivo SQLite se llamará: route_planner_database
* Android lo almacenará en el espacio privado de nuestra aplicación.
* */