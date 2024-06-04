import org.gradle.internal.os.OperatingSystem
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

plugins {
    id("java-library")
}

group = "com.ardor3d"
version = "1.7.0"

val collectJarsDir = layout.buildDirectory.dir("collected-jars")

extra["lwjglVersion"] = "3.3.3"
extra["lwjglNatives"] = when {
    OperatingSystem.current().isWindows -> "natives-windows"
    OperatingSystem.current().isLinux -> "natives-linux"
    OperatingSystem.current().isMacOsX -> "natives-macos"
    else -> throw IllegalStateException("Unsupported operating system")
}

// Disable the JAR task for the root project
tasks.named<Jar>("jar") {
    enabled = false
}

subprojects {
    apply(plugin = "java-library")

    group = "com.ardor3d"
    version = "1.7.0"

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.named<Jar>("jar") {
        archiveBaseName.set(project.name)
        archiveVersion.set(project.version.toString())
        archiveFileName.set("${archiveBaseName.get()}-${archiveVersion.get()}.jar")
        from(sourceSets.main.get().output)
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        }
    }

    tasks.register<Jar>("packageSources") {
        archiveBaseName.set("${project.name}-sources")
        archiveVersion.set(project.version.toString())
        archiveFileName.set("${archiveBaseName.get()}-${archiveVersion.get()}.jar")
        from(sourceSets.main.get().allSource)
    }

    artifacts {
        add("archives", tasks["jar"])
        add("archives", tasks["packageSources"])
    }

    repositories {
        mavenCentral()
        maven("https://repo.maven.apache.org/maven2")
    }

    configurations.all {
        resolutionStrategy {
            eachDependency {
                if (requested.name.contains("\${osgi.platform}")) {
                    when {
                        OperatingSystem.current().isWindows -> {
                            useTarget(requested.toString().replace("\${osgi.platform}", "win32.win32.x86_64:3.108.0"))
                        }

                        OperatingSystem.current().isLinux -> {
                            useTarget(requested.toString().replace("\${osgi.platform}", "gtk.linux.x86_64:3.108.0"))
                        }

                        OperatingSystem.current().isMacOsX -> {
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

tasks.register<Copy>("collectJars") {
	group = "build"
	description = "Collects all JARs from subprojects into a common directory"

	// Clear the destination directory before copying
	doFirst {
		delete(collectJarsDir.get().asFile)
	}

	// Specify the destination directory
	into(collectJarsDir)

	// Collect JAR files from all subprojects
	subprojects.forEach { subproject ->
		from(subproject.tasks.withType<Jar>().map { it.outputs.files })
	}
}

// Ensure the collectJars task runs after the build task
tasks.named("build") {
	finalizedBy("collectJars")
}