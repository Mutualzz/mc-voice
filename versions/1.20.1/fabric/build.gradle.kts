plugins {
    id("dev.architectury.loom") version "1.17.491"
    id("maven-publish")
}

version = "${rootProject.property("mod_version")}+mc1.20.1-fabric"
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
}

loom {
    silentMojangMappingsLicense()
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
    minecraft("com.mojang:minecraft:1.20.1")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.10")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.92.2+1.20.1")

    include(implementation("de.maxhenkel.rnnoise4j:rnnoise4j:2.1.2")!!)
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
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
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
