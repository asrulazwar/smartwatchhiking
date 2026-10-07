package id.asrul.pendaki.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SesiEntity::class, TitikEntity::class, WaypointEntity::class, SampelEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class PendakiDatabase : RoomDatabase() {
    abstract fun sesiDao(): SesiDao
    abstract fun titikDao(): TitikDao
    abstract fun waypointDao(): WaypointDao
    abstract fun sampelDao(): SampelDao
}
