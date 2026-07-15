plugins {
    id("dev.architectury.loom") version "1.17.491"
    id("maven-publish")
}

version = "${rootProject.property("mod_version")}+mc1.18.2-forge"
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
                    "PercentSlider.java",
                    "VoiceSession.java",
                )
            }
        }
        resources {
            srcDir(rootProject.file("common/src/main/resources"))
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.18.2")
    mappings(loom.officialMojangMappings())
    "forge"("net.minecraftforge:forge:1.18.2-40.2.21")

    include(implementation("de.maxhenkel.rnnoise4j:rnnoise4j:2.1.2")!!)
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to "1.18.2",
        "minecraft_version_range" to "[1.18.2,1.19)",
        "forge_version" to "40.2.21",
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
