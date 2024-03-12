import org.gradle.internal.os.OperatingSystem

plugins {
	id("java-library")
}

group = "com.ardor3d"
version = "1.6.2"

extra["lwjglVersion"] = "3.3.3"
extra["lwjglNatives"] = when (OperatingSystem.current()) {
	OperatingSystem.WINDOWS -> "natives-windows"
	OperatingSystem.LINUX -> "natives-linux"
	OperatingSystem.MAC_OS -> "natives-macos"
	else -> throw IllegalStateException("Unsupported operating system")
}

// Configure all projects
allprojects {
	apply(plugin = "java-library")

	java {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	tasks.withType<JavaCompile> {
		options.encoding = "UTF-8"
	}

	tasks.register("packageSources", Jar::class) {
		from(sourceSets.main.get().allSource)
	}

	artifacts.add("archives", tasks["packageSources"])

	repositories {
		mavenCentral()
		maven("https://repo.maven.apache.org/maven2")
	}

	configurations.all {
		resolutionStrategy {
			eachDependency {
				if (requested.name.contains("\${osgi.platform}")) {
					when (OperatingSystem.current()) {
						OperatingSystem.WINDOWS -> {
							useTarget(requested.toString().replace("\${osgi.platform}", "win32.win32.x86_64:3.108.0"))
						}
						OperatingSystem.LINUX -> {
							useTarget(requested.toString().replace("\${osgi.platform}", "gtk.linux.x86_64:3.108.0"))
						}
						OperatingSystem.MAC_OS -> {
							useTarget(requested.toString().replace("\${osgi.platform}", "cocoa.macosx.x86_64:3.108.0"))
						}
					}
				}
			}
		}
	}

	dependencies {
		testImplementation("junit:junit:4.13.2")
		testImplementation("org.easymock:easymock:5.2.0")
	}
}