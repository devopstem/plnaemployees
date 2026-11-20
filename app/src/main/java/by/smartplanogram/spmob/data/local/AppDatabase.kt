package by.smartplanogram.spmob.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import by.smartplanogram.spmob.data.local.dao.ProductDao
import by.smartplanogram.spmob.data.local.dao.ProductLocationDao
import by.smartplanogram.spmob.data.local.dao.RackDao
import by.smartplanogram.spmob.data.local.dao.RecentSearchDao
import by.smartplanogram.spmob.data.local.dao.StoreDao
import by.smartplanogram.spmob.data.local.dao.StoreMapDao
import by.smartplanogram.spmob.data.local.dao.SyncMetaDao
import by.smartplanogram.spmob.data.local.dao.TaskDao
import by.smartplanogram.spmob.data.local.dao.UserDao
import by.smartplanogram.spmob.data.local.entity.ProductEntity
import by.smartplanogram.spmob.data.local.entity.ProductLocationEntity
import by.smartplanogram.spmob.data.local.entity.RackEntity
import by.smartplanogram.spmob.data.local.entity.RecentSearchEntity
import by.smartplanogram.spmob.data.local.entity.StoreEntity
import by.smartplanogram.spmob.data.local.entity.StoreMapCacheEntity
import by.smartplanogram.spmob.data.local.entity.SyncMetaEntity
import by.smartplanogram.spmob.data.local.entity.TaskEntity
import by.smartplanogram.spmob.data.local.entity.UserProfileEntity

@Database(
    entities = [
        StoreEntity::class,
        ProductEntity::class,
        ProductLocationEntity::class,
        RackEntity::class,
        TaskEntity::class,
        StoreMapCacheEntity::class,
        UserProfileEntity::class,
        RecentSearchEntity::class,
        SyncMetaEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stores(): StoreDao
    abstract fun products(): ProductDao
    abstract fun locations(): ProductLocationDao
    abstract fun racks(): RackDao
    abstract fun tasks(): TaskDao
    abstract fun storeMaps(): StoreMapDao
    abstract fun users(): UserDao
    abstract fun recent(): RecentSearchDao
    abstract fun syncMeta(): SyncMetaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "spmob.db",
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
