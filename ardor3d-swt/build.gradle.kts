import org.gradle.internal.os.OperatingSystem

description = "Ardor 3D SWT"

val swtVersion = "3.115.0"
val swtNatives = when (OperatingSystem.current()) {
	OperatingSystem.WINDOWS -> "org.eclipse.swt.win32.win32.x86_64"
	OperatingSystem.LINUX -> "org.eclipse.swt.gtk.linux.x86_64"
	OperatingSystem.MAC_OS -> "org.eclipse.swt.cocoa.macosx.x86_64"
	else -> throw IllegalStateException("Unsupported operating system")
}

dependencies {
	api(project(":ardor3d-core"))
	
	api("org.eclipse.platform:org.eclipse.swt:$swtVersion")

	implementation("org.eclipse.platform:$swtNatives:$swtVersion")
}
