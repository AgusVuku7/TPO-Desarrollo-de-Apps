package com.chargeroute.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
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

        fun getInstance(context: Context): ChargeRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChargeRouteDatabase::class.java,
                    "chargeroute_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
