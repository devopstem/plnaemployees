package by.smartplanogram.plnanote

/**
 * Loads libplna.so (CMake target `plna`: JNI bridges + static plna_core).
 * Safe to touch from several classes — Android ignores a second load.
 */
internal object PlnaNative {
    init {
        System.loadLibrary("plna")
    }

    fun ensureLoaded() = Unit
}
