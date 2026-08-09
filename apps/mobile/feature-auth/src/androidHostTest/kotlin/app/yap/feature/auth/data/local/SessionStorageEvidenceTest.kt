package app.yap.feature.auth.data.local

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural evidence for AC-050: the session key lives in Android Keystore and the module never
 * falls back to `EncryptedSharedPreferences`.
 */
// Detekt's default test-source excludes do not cover the `androidHostTest` source set yet.
@Suppress("FunctionNaming")
internal class SessionStorageEvidenceTest {

    @Test
    fun `GIVEN the feature module WHEN scanning its production sources THEN EncryptedSharedPreferences is absent`() {
        val sourcesUsingEncryptedSharedPreferences = moduleSourceFiles()
            .filter { file -> file.readText().contains("EncryptedSharedPreferences") }
            .map(File::getName)

        assertEquals(expected = emptyList(), actual = sourcesUsingEncryptedSharedPreferences)
    }

    @Test
    fun `GIVEN the production key provider WHEN reading its source THEN the key is generated inside Android Keystore`() {
        val source = moduleSourceFile("KeystoreSessionSecretKeyProvider.kt").readText()

        assertTrue(
            actual = source.contains("\"AndroidKeyStore\"") &&
                source.contains("KeyProperties.BLOCK_MODE_GCM") &&
                source.contains("KeyProperties.ENCRYPTION_PADDING_NONE"),
            message = "the session key must be a Keystore-held AES-GCM key",
        )
    }

    @Test
    fun `GIVEN the production key provider WHEN reading its source THEN no raw key material is constructed`() {
        val source = moduleSourceFile("KeystoreSessionSecretKeyProvider.kt").readText()

        assertEquals(expected = false, actual = source.contains("SecretKeySpec"))
    }
}

private fun moduleSourceFile(name: String): File =
    moduleSourceFiles().first { file -> file.name == name }

private fun moduleSourceFiles(): List<File> = moduleSourceRoot()
    .walkTopDown()
    .filter { file -> file.isFile && file.extension == "kt" }
    .filter { file -> file.invariantSeparatorsPath.contains("Main/kotlin/") }
    .toList()

private fun moduleSourceRoot(): File {
    val candidates = listOf(File("src"), File("apps/mobile/feature-auth/src"))
    return candidates.first(File::isDirectory)
}
