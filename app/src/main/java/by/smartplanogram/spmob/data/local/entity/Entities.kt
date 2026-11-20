package by.smartplanogram.spmob.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val tenantId: String,
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"]),
        Index(value = ["barcode"], unique = true),
    ],
)
data class ProductEntity(
    @PrimaryKey val barcode: String,
    val tenantId: String,
    val name: String,
    val category: String,
    val checked: Boolean,
    val isPrivate: Boolean,
    /** Optional cloud token, e.g. water / soda / milk — see [by.smartplanogram.spmob.ui.product.ProductIconHint]. */
    val iconHint: String? = null,
)

@Entity(
    tableName = "product_locations",
    primaryKeys = ["storeId", "barcode"],
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["barcode"],
            childColumns = ["barcode"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("barcode"), Index("rackId"), Index("zoneCode")],
)
data class ProductLocationEntity(
    val storeId: String,
    val barcode: String,
    val rackId: String,
    val zoneCode: String,
    val shelf: Int,
    val posX: Float,
    val posY: Float,
    val posZ: Float,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    val title: String,
    val barcode: String?,
    val status: String,
    val dueAt: String,
    val priority: Int,
)

@Entity(tableName = "store_map_cache")
data class StoreMapCacheEntity(
    @PrimaryKey val storeId: String,
    val json: String,
    val updatedAt: Long,
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val role: String,
    val storeId: String,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val queriedAt: Long,
)

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Entity(
    tableName = "racks",
    indices = [
        Index("storeId"),
        Index("departmentId"),
        Index(value = ["storeId", "departmentId", "sortOrder"]),
    ],
)
data class RackEntity(
    @PrimaryKey val id: String,
    val storeId: String,
    /** Zone / department code, e.g. WJ, WE. */
    val departmentId: String,
    val code: String,
    /** rack | cooler | stand */
    val equipmentType: String = "rack",
    val posX: Float,
    val posY: Float,
    val widthMeters: Float,
    val depthMeters: Float,
    val heightMeters: Float,
    val shelves: Int,
    val sortOrder: Int,
)

data class ProductSearchRow(
    @Embedded val product: ProductEntity,
    val rackId: String?,
    val zoneCode: String?,
    val shelf: Int?,
    val posX: Float?,
    val posY: Float?,
    val posZ: Float?,
) {
    val hasLocation: Boolean get() = rackId != null
}
