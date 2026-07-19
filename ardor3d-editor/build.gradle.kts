import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

description = "Ardor3D Visual Editor"

val lwjglVersion: String by rootProject.extra
val lwjglNatives: String by rootProject.extra

repositories {
    google()
    mavenCentral()
    // JetBrains Compose repo - used last to avoid rate limiting
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Ardor3D
    implementation(project(":ardor3d-core"))
    implementation(project(":ardor3d-lwjgl3"))
    implementation(project(":ardor3d-lwjgl3-awt"))
    implementation(project(":ardor3d-awt"))  // For AWT input wrappers
    implementation(project(":ardor3d-extras"))  // For interact widgets
    implementation(project(":ardor3d-collada"))  // For COLLADA model import

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Note: Skia classes (DirectContext, Surface, etc.) come transitively via compose.desktop.currentOs

    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}

compose.desktop {
    application {
        mainClass = "com.ardor3d.editor.NativeEditorAppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ardor3d-editor"
            packageVersion = "1.0.0"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    // The composition scale gates hold 50k-spatial scenes; the default 512m test heap is tight
    // enough there that GC would skew the allocation accounting the gates assert on.
    maxHeapSize = "2g"

    // Force Mesa's software rasterizer for the GL smoke tests: WSLg's d3d12 passthrough
    // segfaults natively when a second GL context comes up in the same process, and its
    // framebuffer readback returns zeros anyway. llvmpipe renders these tests correctly and
    // deterministically; on non-Mesa platforms the variable is ignored.
    environment("LIBGL_ALWAYS_SOFTWARE", "1")
}
