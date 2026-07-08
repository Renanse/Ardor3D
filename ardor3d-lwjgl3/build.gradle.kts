
description = "Ardor 3D LWJGL3"

val lwjglVersion = rootProject.extra["lwjglVersion"] as String
val lwjglNatives = rootProject.extra["lwjglNatives"] as String

dependencies {
	api(project(":ardor3d-core"))
  	
	api("org.lwjgl:lwjgl:$lwjglVersion")
	api("org.lwjgl:lwjgl-assimp:$lwjglVersion")
	api("org.lwjgl:lwjgl-glfw:$lwjglVersion")
	api("org.lwjgl:lwjgl-jawt:$lwjglVersion")
	api("org.lwjgl:lwjgl-openal:$lwjglVersion")
	api("org.lwjgl:lwjgl-opengl:$lwjglVersion")
	api("org.lwjgl:lwjgl-stb:$lwjglVersion")

	implementation("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-assimp:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-openal:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
	implementation("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")

	// The GL smoke tests render extras' interact gizmos in a real context; test-only, so this
	// does not add a runtime dependency (extras depends on core, not on lwjgl3).
	testImplementation(project(":ardor3d-extras"))
}