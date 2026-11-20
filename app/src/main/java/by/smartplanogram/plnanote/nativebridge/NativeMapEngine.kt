package by.smartplanogram.plnanote.nativebridge

import androidx.annotation.Keep
import by.smartplanogram.plnanote.PlnaNative

/**
 * Static JNI surface of libplna.so.
 * Names and signatures must match platform/android/jni_map_bridge.cpp.
 */
@Keep
class NativeMapEngine private constructor() {
    companion object {
        init {
            PlnaNative.ensureLoaded()
        }

        const val EQUIP_RACK = 1
        const val EQUIP_COOLER = 2
        const val EQUIP_STAND = 3

        const val MARKER_OTHER = 0
        const val MARKER_BEACON = 1
        const val MARKER_CASH = 2
        const val MARKER_ENTRANCE = 3
        const val MARKER_EXIT = 4

        const val LAYER_RACKS = 1
        const val LAYER_COOLERS = 2
        const val LAYER_STANDS = 3
        const val LAYER_BEACONS = 4

        @JvmStatic
        external fun calculateRoute(fromX: Float, fromY: Float, toX: Float, toY: Float): FloatArray

        @JvmStatic
        external fun drawRack(
            rackId: String,
            startX: Float,
            startY: Float,
            width: Float,
            depth: Float,
            shelves: Int,
            shelfHeight: Float,
        ): FloatArray

        /** x, y, z, shelf — or empty if the barcode is not in the native cache. */
        @JvmStatic
        external fun findProduct(barcode: String): FloatArray

        @JvmStatic
        external fun getCoreVersion(): String

        @JvmStatic
        external fun nativeClearScene()

        @JvmStatic
        external fun nativeSetFloor(widthMeters: Float, heightMeters: Float)

        @JvmStatic
        external fun nativeAddEquipment(
            id: String,
            type: Int,
            x: Float,
            y: Float,
            w: Float,
            d: Float,
            h: Float,
            rotationDeg: Float,
            shelves: Int,
            r: Float,
            g: Float,
            b: Float,
            a: Float,
        )

        @JvmStatic
        external fun nativeAddWall(x1: Float, y1: Float, x2: Float, y2: Float, isDoor: Boolean)

        @JvmStatic
        external fun nativeAddMarker(
            id: String,
            type: Int,
            x: Float,
            y: Float,
            w: Float,
            d: Float,
            rotationDeg: Float,
            r: Float,
            g: Float,
            b: Float,
            a: Float,
        )

        @JvmStatic
        external fun nativeSetLayerVisible(layer: Int, visible: Boolean)

        @JvmStatic
        external fun nativeSetView3D(enabled: Boolean)

        @JvmStatic
        external fun nativeBuildDrawList(): FloatArray

        @JvmStatic
        external fun nativeLoadStoreMapJson(json: String): Boolean

        @JvmStatic
        external fun nativeRevision(): Long

        @JvmStatic
        external fun nativePutProduct(barcode: String, x: Float, y: Float, z: Float, shelf: Float)

        /** width, height in meters. */
        @JvmStatic
        external fun nativeFloorSize(): FloatArray
    }
}
