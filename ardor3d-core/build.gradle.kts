
description = "Ardor 3D Core"

val snakeyamlVersion = "2.2"

dependencies {
	api(project(":ardor3d-math"))

	implementation("org.yaml:snakeyaml:$snakeyamlVersion")
}
