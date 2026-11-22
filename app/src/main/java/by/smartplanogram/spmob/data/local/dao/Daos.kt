package by.smartplanogram.spmob.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import by.smartplanogram.spmob.data.local.entity.ProductEntity
import by.smartplanogram.spmob.data.local.entity.ProductLocationEntity
import by.smartplanogram.spmob.data.local.entity.ProductSearchRow
import by.smartplanogram.spmob.data.local.entity.RackEntity
import by.smartplanogram.spmob.data.local.entity.RecentSearchEntity
import by.smartplanogram.spmob.data.local.entity.StoreEntity
import by.smartplanogram.spmob.data.local.entity.StoreMapCacheEntity
import by.smartplanogram.spmob.data.local.entity.SyncMetaEntity
import by.smartplanogram.spmob.data.local.entity.TaskEntity
import by.smartplanogram.spmob.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clear()

    @Query(
        """
        SELECT p.*,
               l.rackId AS rackId,
               l.zoneCode AS zoneCode,
               l.shelf AS shelf,
               l.posX AS posX,
               l.posY AS posY,
               l.posZ AS posZ
        FROM products p
        LEFT JOIN product_locations l
          ON l.barcode = p.barcode AND l.storeId = :storeId
        WHERE (:query = '' OR p.name LIKE '%' || :query || '%' OR p.barcode LIKE '%' || :query || '%')
          AND (:category = '' OR p.category = :category)
          AND (:zoneCode = '' OR l.zoneCode = :zoneCode)
          AND (
            :placeType = ''
            OR EXISTS (
              SELECT 1 FROM racks r
              WHERE r.id = l.rackId AND r.storeId = :storeId AND r.equipmentType = :placeType
            )
          )
        ORDER BY p.name
        LIMIT :limit
        """,
    )
    suspend fun search(
        storeId: String,
        query: String,
        category: String,
        zoneCode: String = "",
        placeType: String = "",
        limit: Int = 10_000,
    ): List<ProductSearchRow>

    @Query(
        """
        SELECT COUNT(*)
        FROM products p
        LEFT JOIN product_locations l
          ON l.barcode = p.barcode AND l.storeId = :storeId
        WHERE (:query = '' OR p.name LIKE '%' || :query || '%' OR p.barcode LIKE '%' || :query || '%')
          AND (:category = '' OR p.category = :category)
          AND (:zoneCode = '' OR l.zoneCode = :zoneCode)
          AND (
            :placeType = ''
            OR EXISTS (
              SELECT 1 FROM racks r
              WHERE r.id = l.rackId AND r.storeId = :storeId AND r.equipmentType = :placeType
            )
          )
        """,
    )
    suspend fun searchCount(
        storeId: String,
        query: String,
        category: String,
        zoneCode: String = "",
        placeType: String = "",
    ): Int

    @Query(
        """
        SELECT p.*,
               l.rackId AS rackId,
               l.zoneCode AS zoneCode,
               l.shelf AS shelf,
               l.posX AS posX,
               l.posY AS posY,
               l.posZ AS posZ
        FROM products p
        LEFT JOIN product_locations l
          ON l.barcode = p.barcode AND l.storeId = :storeId
        WHERE p.barcode = :barcode
        LIMIT 1
        """,
    )
    suspend fun byBarcode(storeId: String, barcode: String): ProductSearchRow?

    @Query("SELECT DISTINCT category FROM products ORDER BY category")
    suspend fun categories(): List<String>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}

@Dao
interface ProductLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ProductLocationEntity>)

    @Query("DELETE FROM product_locations")
    suspend fun clear()

    @Query("SELECT * FROM product_locations WHERE storeId = :storeId")
    suspend fun forStore(storeId: String): List<ProductLocationEntity>

    @Query("SELECT * FROM product_locations WHERE storeId = :storeId AND rackId = :rackId ORDER BY shelf DESC, barcode")
    suspend fun forRack(storeId: String, rackId: String): List<ProductLocationEntity>
}

@Dao
interface StoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(store: StoreEntity)

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): StoreEntity?

    @Query("SELECT * FROM stores LIMIT 1")
    suspend fun first(): StoreEntity?
}

@Dao
interface StoreMapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(map: StoreMapCacheEntity)

    @Query("SELECT * FROM store_map_cache WHERE storeId = :storeId LIMIT 1")
    suspend fun byStore(storeId: String): StoreMapCacheEntity?
}

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun clear()

    @Query("SELECT * FROM tasks WHERE storeId = :storeId ORDER BY priority, dueAt")
    fun observe(storeId: String): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE storeId = :storeId
          AND (
            (:filter = 'today' AND status = 'open' AND date(dueAt) = date('now','localtime'))
            OR (:filter = 'overdue' AND status = 'open' AND datetime(dueAt) < datetime('now','localtime'))
            OR (:filter = 'done' AND status = 'done')
            OR (:filter = 'all')
          )
        ORDER BY priority, dueAt
        """,
    )
    suspend fun filtered(storeId: String, filter: String): List<TaskEntity>

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE storeId = :storeId AND status = 'open'
          AND date(dueAt) = date('now','localtime')
        """,
    )
    fun observeOpenTodayCount(storeId: String): Flow<Int>

    @Query("UPDATE tasks SET status = 'done' WHERE id = :id")
    suspend fun markDone(id: String)
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserProfileEntity)

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun current(): UserProfileEntity?

    @Query("DELETE FROM user_profile")
    suspend fun clear()
}

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecentSearchEntity)

    @Query("SELECT * FROM recent_searches ORDER BY queriedAt DESC LIMIT :limit")
    fun observe(limit: Int = 10): Flow<List<RecentSearchEntity>>

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Dao
interface SyncMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncMetaEntity)

    @Query("SELECT value FROM sync_meta WHERE key = :key LIMIT 1")
    suspend fun get(key: String): String?
}

@Dao
interface RackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RackEntity>)

    @Query("DELETE FROM racks")
    suspend fun clear()

    @Query(
        """
        SELECT * FROM racks
        WHERE storeId = :storeId AND departmentId = :departmentId
        ORDER BY sortOrder ASC, posX ASC
        """,
    )
    suspend fun byDepartment(storeId: String, departmentId: String): List<RackEntity>

    @Query("SELECT * FROM racks WHERE id = :rackId LIMIT 1")
    suspend fun byId(rackId: String): RackEntity?

    @Query(
        """
        SELECT departmentId FROM racks
        WHERE storeId = :storeId
        GROUP BY departmentId
        ORDER BY departmentId
        """,
    )
    suspend fun departmentIds(storeId: String): List<String>

    @Query(
        """
        SELECT COUNT(*) FROM racks
        WHERE storeId = :storeId AND departmentId = :departmentId
        """,
    )
    suspend fun countInDepartment(storeId: String, departmentId: String): Int
}
