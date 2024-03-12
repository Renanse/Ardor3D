
description = "Ardor 3D LWJGL3 Nuklear"

val lwjglVersion: String? = project.findProperty("lwjglVersion") as String?
val lwjglNatives: String? = project.findProperty("lwjglNatives") as String?

dependencies {
	api(project(":ardor3d-lwjgl3"))

	api("org.lwjgl:lwjgl-nuklear:$lwjglVersion")

	implementation("org.lwjgl:lwjgl-nuklear:$lwjglVersion:$lwjglNatives")
}