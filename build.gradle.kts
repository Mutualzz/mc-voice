plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String
base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven")
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("mutualzz_voice") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    // Amecs API jars (extracted from the Modrinth bundle JiJ). See libs/README.
    "clientCompileOnly"(fileTree("libs") { include("amecs-*.jar") })
    // Full Amecs for runClient — players install Amecs next to this mod (depends: amecsapi).
    "localRuntime"("maven.modrinth:amecs:${property("amecs_version")}")

    // Dev-only: Microsoft login from runClient (not shipped in the release jar)
    "clientRuntimeOnly"("maven.modrinth:auth-me:v9.2.1+26.1")
}

tasks.processResources {
    val version = project.version
    inputs.property("version", version)
    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.name}" }
    }
}
