package app.yap.feature.auth.presentation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val APP_ROOT_SOURCES = "apps/mobile/app-root/src"
private const val FEATURE_AUTH_SOURCES = "apps/mobile/feature-auth/src"
private const val LOGIN_PACKAGE = "/presentation/login/"
private const val SELECT_PROVIDER_PACKAGE = "/presentation/selectprovider/"

private val NAVIGATION_PRIMITIVES = listOf(
    "ChildSlot",
    "childSlot",
    "SlotNavigation",
    "ChildStack",
    "childStack",
    "StackNavigation",
)

private val REPOSITORY_ROOT: File = generateSequence(File("").absoluteFile) { file -> file.parentFile }
    .first { file -> File(file, "settings.gradle.kts").isFile }

/**
 * Static evidence for the screen isolation rules: neither screen names the other, the navigation
 * primitive holding `SelectProvider` is declared by `Auth`, and the application root knows about
 * neither screen (R-088, R-093, R-094, AC-060, AC-061).
 */
// Detekt's default test-source excludes do not cover the `androidHostTest` source set yet.
@Suppress("FunctionNaming")
internal class ScreenIsolationTest {

    @Test
    fun `GIVEN the Login sources WHEN scanning them THEN they never name SelectProvider`() {
        val sources = kotlinSourcesIn(module = FEATURE_AUTH_SOURCES, packagePath = LOGIN_PACKAGE)

        val offenders = sources.filter { source -> source.readText().contains("SelectProvider") }

        assertEquals(expected = emptyList(), actual = offenders.map { offender -> offender.name })
    }

    @Test
    fun `GIVEN the Login sources WHEN scanning them THEN they declare no slot or stack navigation`() {
        val sources = kotlinSourcesIn(module = FEATURE_AUTH_SOURCES, packagePath = LOGIN_PACKAGE)

        val offenders = sources.filter { source -> source.readText().containsNavigationPrimitive() }

        assertEquals(expected = emptyList(), actual = offenders.map { offender -> offender.name })
    }

    @Test
    fun `GIVEN the SelectProvider sources WHEN scanning them THEN they never name LoginComponent`() {
        val sources = kotlinSourcesIn(module = FEATURE_AUTH_SOURCES, packagePath = SELECT_PROVIDER_PACKAGE)

        val offenders = sources.filter { source -> source.readText().contains("LoginComponent") }

        assertEquals(expected = emptyList(), actual = offenders.map { offender -> offender.name })
    }

    @Test
    fun `GIVEN the application root sources WHEN scanning them THEN they name neither screen`() {
        val sources = kotlinSourcesIn(module = APP_ROOT_SOURCES, packagePath = "")

        val offenders = sources.filter { source -> source.readText().namesEitherScreen() }

        assertEquals(expected = emptyList(), actual = offenders.map { offender -> offender.name })
    }

    @Test
    fun `GIVEN the screen packages WHEN scanning them THEN both of them hold Kotlin sources`() {
        val login = kotlinSourcesIn(module = FEATURE_AUTH_SOURCES, packagePath = LOGIN_PACKAGE)
        val selectProvider = kotlinSourcesIn(module = FEATURE_AUTH_SOURCES, packagePath = SELECT_PROVIDER_PACKAGE)

        assertTrue(login.isNotEmpty() && selectProvider.isNotEmpty())
    }
}

private fun String.containsNavigationPrimitive(): Boolean =
    NAVIGATION_PRIMITIVES.any { primitive -> contains(primitive) }

private fun String.namesEitherScreen(): Boolean =
    contains("LoginComponent") || contains("SelectProviderComponent")

private fun kotlinSourcesIn(module: String, packagePath: String): List<File> =
    File(REPOSITORY_ROOT, module)
        .walkTopDown()
        .filter { file -> file.isFile && file.extension == "kt" }
        .filter { file -> file.invariantSeparatorsPath.contains(packagePath) }
        .toList()
