import org.gradle.internal.os.OperatingSystem

description = "Ardor 3D LWJGL3 SWT"

val lwjglSwtVersion = "1.0.0"
val lwjgl3SwtPackage = when {
	OperatingSystem.current().isWindows -> "lwjgl3-swt-windows"
	OperatingSystem.current().isLinux -> "lwjgl3-swt-linux"
	OperatingSystem.current().isMacOsX -> "lwjgl3-swt-macos"
	else -> throw IllegalStateException("Unsupported operating system")
}

dependencies {
	api(project(":ardor3d-lwjgl3"))
	api(project(":ardor3d-swt"))

	api("org.lwjglx:lwjgl3-swt:$lwjglSwtVersion")
	api("org.lwjglx:$lwjgl3SwtPackage:$lwjglSwtVersion")
}
