
description = "Ardor 3D LWJGL3 AWT"

// Needs 0.2.4+ with LWJGL 3.4: the 3.4 Windows bindings changed shape (e.g.
// User32.RegisterClassEx grew an error-code IntBuffer parameter), so older
// lwjgl3-awt jars fail at runtime on Windows with NoSuchMethodError.
val lwjglAwtVersion = "0.2.4"

dependencies {
	api(project(":ardor3d-lwjgl3"))
	api(project(":ardor3d-awt"))
	api("org.lwjglx:lwjgl3-awt:$lwjglAwtVersion") {
		// The lwjgl3-awt POM picks LWJGL natives classifiers via Maven OS profiles,
		// which Gradle doesn't evaluate (the classifier stays "${lwjgl.natives}" and
		// resolution fails). ardor3d-lwjgl3 already supplies lwjgl, lwjgl-opengl,
		// lwjgl-jawt and the natives, so skip its transitive dependencies entirely.
		isTransitive = false
	}
}
