package by.smartplanogram.spmob.data.auth

/**
 * Built-in staff credentials kept in bytecode (not assets / not Firebase).
 * Values are XOR-masked so they are not plain strings in resources.
 *
 * Temporary bridge until real accounts exist in Firebase Auth / RTDB.
 * Obfuscation only — not cryptographic secrecy.
 */
internal object EmbeddedLocalAuth {
    const val LOCAL_UID = "local-embedded-merch"

    /** Masked login code units XOR [KEY]. */
    private val LOGIN_MASKED = intArrayOf(0x16, 0x1E, 0x09, 0x18, 0x13)

    /** Masked password code units XOR [KEY]. */
    private val PASS_MASKED = intArrayOf(
        0x28, 0x0B, 0x16, 0x14, 0x19, 0x58, 0x49, 0x4B, 0x49, 0x4D,
    )

    private const val KEY = 0x7B

    fun matches(loginRaw: String, password: String): Boolean {
        val login = loginRaw.trim().lowercase().substringBefore('@')
        return login == reveal(LOGIN_MASKED) && password == reveal(PASS_MASKED)
    }

    private fun reveal(masked: IntArray): String =
        buildString(masked.size) {
            masked.forEach { append((it xor KEY).toChar()) }
        }
}
