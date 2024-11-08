
description = "Ardor 3D Collada Importer"

val jdomVersion = "2.0.6.1"
val jaxenVersion = "2.0.0"

dependencies {
	api(project(":ardor3d-core"))
	api(project(":ardor3d-animation"))
	
	implementation("org.jdom:jdom2:$jdomVersion")
	implementation("jaxen:jaxen:$jaxenVersion")
}
