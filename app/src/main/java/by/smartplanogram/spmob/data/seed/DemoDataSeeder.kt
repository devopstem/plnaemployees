package by.smartplanogram.spmob.data.seed

import android.content.Context
import androidx.room.withTransaction
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.local.entity.ProductEntity
import by.smartplanogram.spmob.data.local.entity.ProductLocationEntity
import by.smartplanogram.spmob.data.local.entity.RackEntity
import by.smartplanogram.spmob.data.local.entity.StoreEntity
import by.smartplanogram.spmob.data.local.entity.StoreMapCacheEntity
import by.smartplanogram.spmob.data.local.entity.SyncMetaEntity
import by.smartplanogram.spmob.data.local.entity.TaskEntity
import by.smartplanogram.spmob.data.local.entity.UserProfileEntity
import org.json.JSONObject

object DemoDataSeeder {
    const val META_KEY = "demo_seed_version"
    private const val LOCAL_ASSET = "demo/plna_demo_bundle.json"

    suspend fun ensureSeeded(context: Context, db: AppDatabase) {
        val jsonText = context.assets.open(LOCAL_ASSET).bufferedReader().use { it.readText() }
        val version = JSONObject(jsonText).getJSONObject("meta").optInt("version", 1).toString()
        if (db.syncMeta().get(META_KEY) == version && db.products().count() > 0) {
            return
        }
        applyBundleJson(context, db, jsonText)
    }

    /** Apply a full catalog JSON bundle into Room (local seed or cloud sync). */
    @Suppress("UNUSED_PARAMETER")
    suspend fun applyBundleJson(context: Context, db: AppDatabase, jsonText: String) {
        val root = JSONObject(jsonText)
        val version = root.getJSONObject("meta").optInt("version", 1).toString()
        val storeJson = root.getJSONObject("store")
        val userJson = root.getJSONObject("user")
        val tenantId = storeJson.getString("tenantId")
        val storeId = storeJson.getString("id")
        val storeMap = root.getJSONObject("storeMap")

        val products = buildList {
            val arr = root.getJSONArray("products")
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                add(
                    ProductEntity(
                        barcode = p.getString("barcode"),
                        tenantId = tenantId,
                        name = p.getString("name"),
                        category = p.getString("category"),
                        checked = p.optBoolean("checked", false),
                        isPrivate = p.optBoolean("isPrivate", false),
                        iconHint = p.optString("iconHint").ifBlank { null },
                    ),
                )
            }
        }

        val locations = buildList {
            val arr = root.getJSONArray("productLocations")
            for (i in 0 until arr.length()) {
                val l = arr.getJSONObject(i)
                add(
                    ProductLocationEntity(
                        storeId = l.getString("storeId"),
                        barcode = l.getString("barcode"),
                        rackId = l.getString("rackId"),
                        zoneCode = l.optString("zoneCode", ""),
                        shelf = l.optInt("shelf", 0),
                        posX = l.optDouble("posX", 0.0).toFloat(),
                        posY = l.optDouble("posY", 0.0).toFloat(),
                        posZ = l.optDouble("posZ", 0.0).toFloat(),
                    ),
                )
            }
        }

        val tasks = buildList {
            val arr = root.optJSONArray("tasks") ?: return@buildList
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                add(
                    TaskEntity(
                        id = t.getString("id"),
                        storeId = t.getString("storeId"),
                        title = t.getString("title"),
                        barcode = t.optString("barcode").ifBlank { null },
                        status = t.getString("status"),
                        dueAt = t.getString("dueAt"),
                        priority = t.optInt("priority", 99),
                    ),
                )
            }
        }

        val rackDrafts = buildList {
            fun read(key: String) {
                val arr = storeMap.optJSONArray(key) ?: return
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Triple(
                            o.optDouble("posX", 0.0).toFloat(),
                            o.optString("id"),
                            o,
                        ),
                    )
                }
            }
            read("racks")
            read("coolers")
            read("stands")
        }.sortedBy { it.first }

        val racks = rackDrafts.mapIndexed { index, (_, _, o) ->
            RackEntity(
                id = o.optString("id"),
                storeId = storeId,
                departmentId = o.optString("zoneCode", "GEN"),
                code = o.optString("id"),
                equipmentType = o.optString("equipmentType", "rack").ifBlank { "rack" },
                posX = o.optDouble("posX", 0.0).toFloat(),
                posY = o.optDouble("posY", 0.0).toFloat(),
                widthMeters = o.optDouble("widthMeters", 1.0).toFloat(),
                depthMeters = o.optDouble("lengthMeters", 0.7).toFloat(),
                heightMeters = o.optDouble("heightMeters", 1.8).toFloat(),
                shelves = o.optInt("shelfCount", 5),
                sortOrder = index,
            )
        }

        db.withTransaction {
            db.products().clear()
            db.locations().clear()
            db.tasks().clear()
            db.racks().clear()

            db.stores().upsert(
                StoreEntity(
                    id = storeId,
                    name = storeJson.getString("name"),
                    address = storeJson.getString("address"),
                    city = storeJson.getString("city"),
                    latitude = storeJson.getDouble("latitude"),
                    longitude = storeJson.getDouble("longitude"),
                    radiusMeters = storeJson.getInt("radiusMeters"),
                    tenantId = tenantId,
                ),
            )
            db.users().upsert(
                UserProfileEntity(
                    uid = userJson.getString("uid"),
                    name = userJson.getString("name"),
                    role = userJson.getString("role"),
                    storeId = userJson.getString("storeId"),
                ),
            )
            db.products().upsertAll(products)
            db.locations().upsertAll(locations)
            db.racks().upsertAll(racks)
            if (tasks.isNotEmpty()) db.tasks().upsertAll(tasks)
            db.storeMaps().upsert(
                StoreMapCacheEntity(
                    storeId = storeId,
                    json = storeMap.toString(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            db.syncMeta().upsert(SyncMetaEntity(META_KEY, version))
        }
    }
}
