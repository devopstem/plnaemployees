package by.smartplanogram.spmob.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Live pull from Firebase Realtime Database for the signed-in user.
 */
object FirebaseCatalogClient {

    suspend fun fetchRootJson(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Нужна авторизация Firebase")
        // Ensure auth token is fresh for RTDB rules (auth != null)
        user.getIdToken(false).await()
        val snapshot = FirebaseDatabase.getInstance().reference.get().await()
        return snapshotToJson(snapshot).toString()
    }

    private fun snapshotToJson(snapshot: DataSnapshot): Any {
        if (!snapshot.exists()) return JSONObject.NULL
        if (!snapshot.hasChildren()) {
            return snapshot.value ?: JSONObject.NULL
        }
        val children = snapshot.children.toList()
        val allNumeric = children.all { it.key?.toIntOrNull() != null }
        if (allNumeric && children.isNotEmpty()) {
            val sorted = children.sortedBy { it.key!!.toInt() }
            val contiguous = sorted.first().key!!.toInt() == 0 &&
                sorted.last().key!!.toInt() == sorted.size - 1
            if (contiguous) {
                return JSONArray().apply {
                    sorted.forEach { put(snapshotToJson(it)) }
                }
            }
        }
        return JSONObject().apply {
            children.forEach { child ->
                val key = child.key ?: return@forEach
                put(key, snapshotToJson(child))
            }
        }
    }
}
