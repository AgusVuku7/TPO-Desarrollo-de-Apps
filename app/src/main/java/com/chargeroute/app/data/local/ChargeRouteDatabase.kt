package com.chargeroute.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chargeroute.app.data.local.dao.RouteDao
import com.chargeroute.app.data.local.dao.StationDao
import com.chargeroute.app.data.local.dao.TripDao
import com.chargeroute.app.data.local.dao.TripStopDao
import com.chargeroute.app.data.local.entity.RouteEntity
import com.chargeroute.app.data.local.entity.StationEntity
import com.chargeroute.app.data.local.entity.TripEntity
import com.chargeroute.app.data.local.entity.TripStopEntity

@Database(
    entities = [
        TripEntity::class,
        RouteEntity::class,
        StationEntity::class,
        TripStopEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ChargeRouteDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao
    abstract fun routeDao(): RouteDao
    abstract fun stationDao(): StationDao
    abstract fun tripStopDao(): TripStopDao

    companion object {
        @Volatile
        private var INSTANCE: ChargeRouteDatabase? = null

        // Version 1 -> 2: se agrega la distancia directa de la ruta.
        // La migracion conserva los viajes ya guardados (las rutas viejas quedan con 0 = sin dato).
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routes ADD COLUMN directDistanceKm REAL NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): ChargeRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChargeRouteDatabase::class.java,
                    "chargeroute_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
