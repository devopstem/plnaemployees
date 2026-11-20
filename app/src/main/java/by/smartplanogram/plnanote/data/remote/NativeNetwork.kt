package by.smartplanogram.plnanote.data.remote

import android.os.Handler
import android.os.Looper
import androidx.annotation.Keep
import by.smartplanogram.plnanote.PlnaNative

/**
 * Static JNI surface of libplna.so.
 * Names and signatures must match platform/android/jni_network_bridge.cpp.
 *
 * Native code calls [onDataReceived] and [onConnectionStatusChanged] on the socket thread.
 * Listeners are delivered on the main thread.
 */
@Keep
class NativeNetwork private constructor() {
    interface Listener {
        fun onDataReceived(type: Int, body: ByteArray) {}
        fun onConnectionStatusChanged(connected: Boolean) {}
    }

    companion object {
        const val PACKET_AUTH = 1
        const val PACKET_SYNC_PLANOGRAM = 2
        const val PACKET_NEW_TASK = 3
        const val PACKET_TASK_STATUS = 4
        const val PACKET_HEARTBEAT = 5

        private val mainHandler = Handler(Looper.getMainLooper())

        @Volatile
        private var listener: Listener? = null

        init {
            PlnaNative.ensureLoaded()
        }

        fun setListener(listener: Listener?) {
            this.listener = listener
        }

        @JvmStatic
        external fun nativeInit()

        @JvmStatic
        external fun nativeConnect(host: String, port: Int): Boolean

        @JvmStatic
        external fun nativeDisconnect()

        @JvmStatic
        external fun nativeShutdown()

        @JvmStatic
        external fun nativeSend(type: Int, data: ByteArray)

        @JvmStatic
        external fun nativeGetBytesSent(): Long

        @JvmStatic
        external fun nativeGetBytesReceived(): Long

        @JvmStatic
        external fun nativeResetStats()

        @JvmStatic
        external fun nativeTriggerHeartbeat()

        @Keep
        @JvmStatic
        fun onDataReceived(type: Int, data: ByteArray) {
            val copy = data.copyOf()
            val current = listener
            mainHandler.post { current?.onDataReceived(type, copy) }
        }

        @Keep
        @JvmStatic
        fun onConnectionStatusChanged(connected: Boolean) {
            val current = listener
            mainHandler.post { current?.onConnectionStatusChanged(connected) }
        }
    }
}
