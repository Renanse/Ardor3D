
description = "Ardor 3D LWJGL3 AWT"

val lwjglAwtVersion = "0.1.8"

dependencies {
	api(project(":ardor3d-lwjgl3"))
	api(project(":ardor3d-awt"))
	api("org.lwjglx:lwjgl3-awt:$lwjglAwtVersion")
}
