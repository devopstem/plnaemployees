package by.smartplanogram.spmob.data.firebase

import androidx.room.withTransaction
import by.smartplanogram.spmob.data.local.AppDatabase
import by.smartplanogram.spmob.data.local.entity.ProductEntity
import by.smartplanogram.spmob.data.local.entity.ProductLocationEntity
import by.smartplanogram.spmob.data.local.entity.RackEntity
import by.smartplanogram.spmob.data.local.entity.StoreEntity
import by.smartplanogram.spmob.data.local.entity.StoreMapCacheEntity
import by.smartplanogram.spmob.data.local.entity.SyncMetaEntity
import by.smartplanogram.spmob.data.local.entity.TaskEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Imports a Firebase Realtime Database dump into Room.
 *
 * Cloud shape:
 * ```
 * catalog/tenants/{tenantId}/products/{id}
 * stores/{storeId}/stores_index/{storeId}
 * stores/{storeId}/store_maps/{storeId}/zones|zoneDictionary|…
 * users/{uid}  — profile (handled by AuthRepository, not overwritten here)
 * ```
 *
 * Racks/locations are synthesized from zones when absent in RTDB.
 */
object FirebaseRtdbImporter {
    const val META_KEY = "firebase_rtdb_version"
    const val DEFAULT_DATABASE_URL = "https://spmob-120212-default-rtdb.firebaseio.com"

    fun extractVersion(jsonText: String): String {
        val root = JSONObject(jsonText)
        val tenants = root.optJSONObject("catalog")?.optJSONObject("tenants") ?: return "0"
        val keys = tenants.keys()
        if (!keys.hasNext()) return "0"
        val meta = tenants.getJSONObject(keys.next()).optJSONObject("metadata")
        return meta?.optString("updatedAt").orEmpty().ifBlank {
            meta?.optInt("count", 0)?.toString() ?: "0"
        }
    }

    suspend fun applyRtdbJson(db: AppDatabase, jsonText: String) {
        val root = JSONObject(jsonText)
        val version = extractVersion(jsonText)

        val tenants = root.getJSONObject("catalog").getJSONObject("tenants")
        val tenantId = tenants.keys().asSequence().first()
        val tenantNode = tenants.getJSONObject(tenantId)
        val productsNode = tenantNode.getJSONObject("products")

        val storesRoot = root.getJSONObject("stores")
        val storeId = storesRoot.keys().asSequence().first()
        val storeBucket = storesRoot.getJSONObject(storeId)
        val storeInfo = storeBucket
            .getJSONObject("stores_index")
            .getJSONObject(storeId)
        val storeMap = storeBucket
            .getJSONObject("store_maps")
            .getJSONObject(storeId)

        val zones = parseZones(storeMap)
        val zoneDictionary = JSONObject().apply {
            zones.forEach { put(it.code, it.name) }
        }
        val zoneColors = JSONObject().apply {
            zones.forEach { put(it.code, it.color) }
        }

        val products = buildList {
            val keys = productsNode.keys()
            while (keys.hasNext()) {
                val p = productsNode.getJSONObject(keys.next())
                val barcode = p.optString("barcode").trim()
                if (barcode.isEmpty()) continue
                val name = p.optString("name").ifBlank { barcode }
                val category = p.optString("category").ifBlank { "Прочие" }
                add(
                    ProductEntity(
                        barcode = barcode,
                        tenantId = tenantId,
                        name = name,
                        category = category,
                        checked = p.optBoolean("checked", false),
                        isPrivate = p.optBoolean("is_private", p.optBoolean("isPrivate", false)),
                        // Icons resolved at UI time from name+category (avoids stale wrong hints).
                        iconHint = null,
                    ),
                )
            }
        }.sortedBy { it.name }

        val racks = synthesizeRacks(storeId, zones)
        val locations = synthesizeLocations(storeId, products, racks, zones)
        val tasks = synthesizeTasks(storeId, products, locations)

        val enrichedMap = JSONObject().apply {
            put("wallPoints", storeMap.optJSONArray("wallPoints") ?: JSONArray())
            put("zones", storeMap.optJSONArray("zones") ?: JSONArray())
            put("zoneDictionary", zoneDictionary)
            put("zoneColors", zoneColors)
            put("racks", JSONArray().apply {
                racks.forEach { r ->
                    put(
                        JSONObject()
                            .put("id", r.id)
                            .put("zoneCode", r.departmentId)
                            .put("equipmentType", r.equipmentType)
                            .put("posX", r.posX.toDouble())
                            .put("posY", r.posY.toDouble())
                            .put("widthMeters", r.widthMeters.toDouble())
                            .put("lengthMeters", r.depthMeters.toDouble())
                            .put("heightMeters", r.heightMeters.toDouble())
                            .put("shelfCount", r.shelves),
                    )
                }
            })
            put("coolers", JSONArray())
            put("stands", JSONArray())
        }

        db.withTransaction {
            db.products().clear()
            db.locations().clear()
            db.tasks().clear()
            db.racks().clear()

            db.stores().upsert(
                StoreEntity(
                    id = storeId,
                    name = storeInfo.optString("name", "Магазин"),
                    address = storeInfo.optString("address", ""),
                    city = storeInfo.optString("city", ""),
                    latitude = storeInfo.optDouble("latitude", 0.0),
                    longitude = storeInfo.optDouble("longitude", 0.0),
                    radiusMeters = storeInfo.optInt("radiusMeters", 100),
                    tenantId = tenantId,
                ),
            )

            products.chunked(400).forEach { db.products().upsertAll(it) }
            racks.chunked(100).forEach { db.racks().upsertAll(it) }
            locations.chunked(400).forEach { db.locations().upsertAll(it) }
            if (tasks.isNotEmpty()) db.tasks().upsertAll(tasks)

            db.storeMaps().upsert(
                StoreMapCacheEntity(
                    storeId = storeId,
                    json = enrichedMap.toString(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            db.syncMeta().upsert(SyncMetaEntity(META_KEY, version))
        }
    }

    private data class ZoneInfo(val code: String, val name: String, val color: String)

    private fun parseZones(storeMap: JSONObject): List<ZoneInfo> {
        val fromArray = storeMap.optJSONArray("zones")
        if (fromArray != null && fromArray.length() > 0) {
            return buildList {
                for (i in 0 until fromArray.length()) {
                    val z = fromArray.getJSONObject(i)
                    val code = z.optString("code").trim()
                    if (code.isEmpty()) continue
                    add(
                        ZoneInfo(
                            code = code,
                            name = z.optString("name", code),
                            color = z.optString("color", "#607D8B"),
                        ),
                    )
                }
            }.sortedBy { it.code }
        }
        val dict = storeMap.optJSONObject("zoneDictionary") ?: return emptyList()
        val colors = storeMap.optJSONObject("zoneColors")
        return dict.keys().asSequence().map { code ->
            ZoneInfo(
                code = code,
                name = dict.optString(code, code),
                color = colors?.optString(code, "#607D8B") ?: "#607D8B",
            )
        }.sortedBy { it.code }.toList()
    }

    private fun synthesizeRacks(storeId: String, zones: List<ZoneInfo>): List<RackEntity> {
        val cols = 6
        return zones.mapIndexed { index, zone ->
            val col = index % cols
            val row = index / cols
            val equipment = when (zone.code.uppercase()) {
                "CED", "WR", "JES", "KSS", "WE", "BEER", "VNO", "AOL", "DAIRY", "IAM" -> "cooler"
                else -> "rack"
            }
            RackEntity(
                id = "R-$storeId-${zone.code}",
                storeId = storeId,
                departmentId = zone.code,
                code = zone.name,
                equipmentType = equipment,
                posX = 1.2f + col * 2.4f,
                posY = 1.0f + row * 2.2f,
                widthMeters = if (equipment == "cooler") 1.2f else 1.8f,
                depthMeters = 0.7f,
                heightMeters = 2.0f,
                shelves = 5,
                sortOrder = index,
            )
        }
    }

    private fun synthesizeLocations(
        storeId: String,
        products: List<ProductEntity>,
        racks: List<RackEntity>,
        zones: List<ZoneInfo>,
    ): List<ProductLocationEntity> {
        if (racks.isEmpty() || products.isEmpty()) return emptyList()
        val rackByZone = racks.associateBy { it.departmentId }
        val zoneList = zones.ifEmpty {
            racks.map { ZoneInfo(it.departmentId, it.code, "#607D8B") }
        }

        val counters = mutableMapOf<String, Int>()

        return products.mapNotNull { product ->
            val zone = resolveZone(product.category, product.name, zoneList) ?: return@mapNotNull null
            val rack = rackByZone[zone.code] ?: return@mapNotNull null
            val slot = counters.merge(zone.code, 1) { a, _ -> a + 1 }!! - 1
            val shelf = 5 - (slot % 5)
            val along = (slot / 5) % 8
            ProductLocationEntity(
                storeId = storeId,
                barcode = product.barcode,
                rackId = rack.id,
                zoneCode = zone.code,
                shelf = shelf,
                posX = rack.posX + along * 0.22f,
                posY = rack.posY,
                posZ = shelf * 0.35f,
            )
        }
    }

    private fun resolveZone(
        category: String,
        name: String,
        zones: List<ZoneInfo>,
    ): ZoneInfo? {
        if (zones.isEmpty()) return null
        val text = "$category $name".lowercase()

        // Direct keyword map for known Firebase zone codes
        val codeHints = listOf(
            "WR" to listOf("вода", "минерал", "питьев"),
            "CED" to listOf("газирован", "лимонад", "кола", "sprite", "фанта"),
            "KSS" to listOf("квас"),
            "WE" to listOf("энергет", "спортпит", "изотон"),
            "JES" to listOf("сок", "нектар", "морс"),
            "BEER" to listOf("пиво", "эль", "лагер"),
            "VNO" to listOf("вино", "игристое", "шампан"),
            "AOL" to listOf("водк", "виски", "коньяк", "алкогол", "ликер", "ром "),
            "DAIRY" to listOf("молоч", "йогурт", "кефир", "творог", "сметан", "ряженк"),
            "CE" to listOf("сыр"),
            "MNE" to listOf("масло слив", "маргарин"),
            "IAM" to listOf("морожен"),
            "EGGS" to listOf("яйц", "куриц", "птиц"),
            "MT" to listOf("мяс", "колбас", "сосис", "ветчин"),
            "SF" to listOf("рыб", "морепрод", "кревет", "лосос"),
            "PDC" to listOf("овощ", "фрукт", "яблок", "банан", "томат", "огурец", "ягод"),
            "BY" to listOf("хлеб", "булк", "батон", "выпечк", "пекар"),
            "CN" to listOf("конфет", "шоколад", "печенье", "вафл", "торт", "пирожн", "кондитер"),
            "NS" to listOf("орех", "восточн", "халв", "лукум"),
            "SNCKS" to listOf("снек", "чипс", "сухар", "попкорн"),
            "SS" to listOf("соус", "приправ", "кетчуп", "майонез", "горчиц"),
            "OILS" to listOf("масло раст", "подсолнеч", "оливков"),
            "DG" to listOf("бакале", "крупа", "макарон", "мука", "сахар", "рис ", "гречк"),
            "CF" to listOf("консерв", "тушенк"),
            "TEA" to listOf("чай", "кофе"),
            "DELI" to listOf("гастроном", "нарезк"),
            "EAT" to listOf("салат", "кулинар"),
            "FRI" to listOf("заморож", "полуфабрик", "пельмен", "вареник"),
            "FGG" to listOf("замороженные овощ", "замороженные ягод"),
            "HLD" to listOf("бытов", "хими", "моющее", "стирк"),
            "PCA" to listOf("косметик", "гигиен", "шампунь", "зубн", "проклад"),
            "PAS" to listOf("бумаж", "туалетн", "салфетк"),
            "FH" to listOf("для дома", "посуд", "хозяйств"),
            "PF" to listOf("животн", "корм", "кошк", "собак"),
            "BC" to listOf("детск", "подгуз", "пюре", "для детей"),
            "BG" to listOf("специ", "хлебопекар", "смес"),
        )

        for ((code, words) in codeHints) {
            if (words.any { it in text }) {
                zones.find { it.code.equals(code, ignoreCase = true) }?.let { return it }
            }
        }

        // Fuzzy: zone display name appears in category
        for (zone in zones) {
            val tokens = zone.name.lowercase()
                .split(Regex("[\\s,]+|\\bи\\b"))
                .map { it.trim() }
                .filter { it.length >= 4 }
            if (tokens.any { it in text }) return zone
        }

        // Stable fallback by category hash so departments fill evenly
        val idx = (category.hashCode().and(0x7fff_ffff)) % zones.size
        return zones[idx]
    }

    private fun synthesizeTasks(
        storeId: String,
        products: List<ProductEntity>,
        locations: List<ProductLocationEntity>,
    ): List<TaskEntity> {
        val placed = locations.map { it.barcode }.toSet()
        val candidates = products.filter { !it.checked && it.barcode in placed }.take(12)
        if (candidates.isEmpty()) return emptyList()
        return candidates.mapIndexed { index, p ->
            TaskEntity(
                id = "task-fb-$index",
                storeId = storeId,
                title = "Проверить выкладку: ${p.name}",
                barcode = p.barcode,
                status = "open",
                dueAt = "2026-11-22T12:00:00",
                priority = index + 1,
            )
        }
    }

}
