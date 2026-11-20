package by.smartplanogram.spmob.data.repo

import by.smartplanogram.plnanote.nativebridge.NativeMapEngine
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.local.entity.ProductSearchRow
import by.smartplanogram.spmob.data.local.entity.RecentSearchEntity
import by.smartplanogram.spmob.data.local.entity.StoreEntity
import by.smartplanogram.spmob.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CatalogRepository(private val db: AppDatabase) {
    suspend fun currentStore(): StoreEntity? = db.stores().first()

    suspend fun currentUser(): UserProfileEntity? = db.users().current()

    suspend fun categories(): List<String> = db.products().categories()

    suspend fun search(
        query: String,
        category: String = "",
        zoneCode: String = "",
        placeType: String = "",
    ): List<ProductSearchRow> {
        val storeId = currentStore()?.id ?: return emptyList()
        return db.products().search(
            storeId = storeId,
            query = query.trim(),
            category = category.trim(),
            zoneCode = zoneCode.trim(),
            placeType = placeType.trim(),
        )
    }

    suspend fun departments(): List<DepartmentInfo> {
        val storeId = currentStore()?.id ?: return emptyList()
        return db.racks().departmentIds(storeId).map { id ->
            DepartmentInfo(
                id = id,
                rackCount = db.racks().countInDepartment(storeId, id),
            )
        }
    }

    suspend fun byBarcode(barcode: String): ProductSearchRow? {
        val storeId = currentStore()?.id ?: return null
        return db.products().byBarcode(storeId, barcode.trim())
    }

    suspend fun rememberSearch(row: ProductSearchRow) {
        db.recent().insert(
            RecentSearchEntity(
                barcode = row.product.barcode,
                name = row.product.name,
                queriedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun recentSearches(limit: Int = 8): Flow<List<RecentSearchEntity>> = db.recent().observe(limit)
}

data class DepartmentInfo(
    val id: String,
    val rackCount: Int,
)

data class RackInfo(
    val id: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val d: Float,
    val h: Float,
    val shelves: Int,
)

class MapRepository(private val db: AppDatabase) {
    /**
     * Loads store_maps JSON from Room into C++ and registers product positions.
     * @return true if map JSON loaded into native core.
     */
    suspend fun applyStoreMapToNative(): Boolean = withContext(Dispatchers.Default) {
        val store = db.stores().first() ?: return@withContext false
        val cache = db.storeMaps().byStore(store.id) ?: return@withContext false
        val ok = NativeMapEngine.nativeLoadStoreMapJson(cache.json)
        if (ok) {
            db.locations().forStore(store.id).forEach { loc ->
                NativeMapEngine.nativePutProduct(
                    loc.barcode,
                    loc.posX,
                    loc.posY,
                    loc.posZ,
                    loc.shelf.toFloat(),
                )
            }
        }
        ok
    }

    suspend fun racks(): List<RackInfo> = withContext(Dispatchers.Default) {
        val store = db.stores().first() ?: return@withContext emptyList()
        val cache = db.storeMaps().byStore(store.id) ?: return@withContext emptyList()
        val root = org.json.JSONObject(cache.json)
        buildList {
            fun readArray(key: String) {
                val arr = root.optJSONArray(key) ?: return
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        RackInfo(
                            id = o.optString("id"),
                            x = o.optDouble("posX", 0.0).toFloat(),
                            y = o.optDouble("posY", 0.0).toFloat(),
                            w = o.optDouble("widthMeters", 1.0).toFloat(),
                            d = o.optDouble("lengthMeters", 0.7).toFloat(),
                            h = o.optDouble("heightMeters", 1.8).toFloat(),
                            shelves = o.optInt("shelfCount", 4),
                        ),
                    )
                }
            }
            readArray("racks")
            readArray("coolers")
            readArray("stands")
        }
    }

    suspend fun productsOnRack(rackId: String): List<Pair<String, by.smartplanogram.spmob.data.local.entity.ProductLocationEntity>> {
        val storeId = db.stores().first()?.id ?: return emptyList()
        return db.locations().forRack(storeId, rackId).map { loc ->
            val name = db.products().byBarcode(storeId, loc.barcode)?.product?.name ?: loc.barcode
            name to loc
        }
    }

    suspend fun mapStatsLine(): String {
        val store = db.stores().first() ?: return "нет магазина"
        val cache = db.storeMaps().byStore(store.id)
        val locations = db.locations().forStore(store.id).size
        val revision = runCatching { NativeMapEngine.nativeRevision() }.getOrDefault(0L)
        return "Room map=${cache != null} · locations=$locations · rev=$revision"
    }
}
