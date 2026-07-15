plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.78"
}

version = "${rootProject.property("mod_version")}+mc1.21.4-neoforge"
group = rootProject.property("maven_group") as String

base {
    archivesName.set("mutualzz-voice")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "henkelmax"
        url = uri("https://maven.maxhenkel.de/repository/public")
    }
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
                    "VoiceSession.java",
                )
            }
        }
        resources {
            srcDir(rootProject.file("common/src/main/resources"))
        }
    }
}

neoForge {
    version = "21.4.137"

    runs {
        create("client") {
            client()
        }
    }

    mods {
        create("mutualzz_voice") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    jarJar(implementation("de.maxhenkel.rnnoise4j:rnnoise4j:2.1.2")!!)
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to "1.21.4",
        "minecraft_version_range" to "[1.21.4,1.22)",
        "neo_version" to "21.4.137",
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
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${project.name}" }
    }
}
