
description = "Ardor 3D LWJGL3 Nuklear"

val lwjglVersion = rootProject.extra["lwjglVersion"] as String
val lwjglNatives = rootProject.extra["lwjglNatives"] as String

dependencies {
	api(project(":ardor3d-lwjgl3"))

	api("org.lwjgl:lwjgl-nuklear:$lwjglVersion")

	implementation("org.lwjgl:lwjgl-nuklear:$lwjglVersion:$lwjglNatives")
}