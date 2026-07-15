plugins {
    id("dev.architectury.loom") version "1.17.491"
    id("maven-publish")
}

version = "${rootProject.property("mod_version")}+mc1.20.4-neoforge"
group = rootProject.property("maven_group") as String

base {
    archivesName.set("mutualzz-voice")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven {
        name = "NeoForged"
        url = uri("https://maven.neoforged.net/releases/")
    }
    maven {
        name = "henkelmax"
        url = uri("https://maven.maxhenkel.de/repository/public")
    }
}

loom {
    silentMojangMappingsLicense()
}

sourceSets {
    main {
        java {
            srcDir(rootProject.file("common/src/main/java"))
            exclude { element ->
                val path = element.file.absoluteFile.invariantSeparatorsPath
                path.contains("/common/src/main/java/") && element.file.name in setOf(
                    "VoicePayload.java",
                    "VoiceClientController.java",
                    "VoiceSettingsScreen.java",
                )
            }
        }
        resources {
            srcDir(rootProject.file("common/src/main/resources"))
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.20.4")
    mappings(loom.officialMojangMappings())
    neoForge("net.neoforged:neoforge:20.4.251")

    include(implementation("de.maxhenkel.rnnoise4j:rnnoise4j:2.1.2")!!)
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to "1.20.4",
        "minecraft_version_range" to "[1.20.4,1.21)",
        "neo_version" to "20.4.251",
        "mod_id" to "mutualzz_voice",
        "mod_name" to "Mutualzz Voice",
        "mod_license" to "LicenseRef-Mutualzz-Proprietary",
        "mod_version" to version,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}

sourceSets.main.get().resources.srcDir(generateModMetadata)

tasks.processResources {
    dependsOn(generateModMetadata)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${project.name}" }
    }
}
