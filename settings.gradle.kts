pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases/")
        }
        maven {
            name = "Architectury"
            url = uri("https://maven.architectury.dev/")
        }
        maven {
            name = "Forge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            name = "KikuGie"
            url = uri("https://maven.kikugie.dev/releases")
        }
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "fabric-loom") {
                useModule("net.fabricmc:fabric-loom:${requested.version}")
            }
            if (requested.id.id == "net.fabricmc.fabric-loom") {
                useModule("net.fabricmc:fabric-loom:${requested.version}")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("dev.kikugie.stonecutter") version "0.9.6" apply false
}

rootProject.name = "mutualzz-voice"

/** Layout: versions/<minecraft>/<loader>/ — Gradle project name is "<mc>-<loader>". */
fun includeVoice(mc: String, loader: String) {
    val id = "$mc-$loader"
    include(id)
    project(":$id").projectDir = file("versions/$mc/$loader")
}

includeVoice("26.1.2", "fabric")
includeVoice("26.1.2", "neoforge")
includeVoice("1.21.4", "fabric")
includeVoice("1.21.4", "neoforge")
includeVoice("1.21.1", "fabric")
includeVoice("1.21.1", "neoforge")
includeVoice("1.20.1", "forge")

includeVoice("1.20.1", "fabric")
includeVoice("1.20.4", "fabric")
includeVoice("1.20.4", "neoforge")
includeVoice("1.19.4", "fabric")
includeVoice("1.19.4", "forge")
includeVoice("1.19.2", "fabric")
includeVoice("1.19.2", "forge")
includeVoice("1.18.2", "fabric")
includeVoice("1.18.2", "forge")
