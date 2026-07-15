// Stonecutter buildscript candidate (coexistence: active project still uses fabric/build.gradle.kts)
plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("maven-publish")
}

version = rootProject.property("mod_version") as String
group = rootProject.property("maven_group") as String

base {
    archivesName.set("mutualzz-voice")
}

repositories {
    mavenCentral()
    maven {
        name = "henkelmax"
        url = uri("https://maven.maxhenkel.de/repository/public")
    }
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

sourceSets {
    named("client") {
        java {
            srcDir(rootProject.file("common/src/main/java"))
        }
        resources {
            srcDir(rootProject.file("common/src/main/resources"))
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${rootProject.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${rootProject.property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")

    include(implementation("de.maxhenkel.rnnoise4j:rnnoise4j:2.1.2")!!)

    "clientCompileOnly"(fileTree("libs") { include("amecs-*.jar") })
    "localRuntime"("maven.modrinth:amecs:${rootProject.property("amecs_version")}")
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
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${project.name}" }
    }
}
