plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.141"
}

version = "${rootProject.property("mod_version")}+mc26.1.2-neoforge"
group = rootProject.property("maven_group") as String

base {
    archivesName.set("mutualzz-voice")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
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
        }
        resources {
            srcDir(rootProject.file("common/src/main/resources"))
        }
    }
}

neoForge {
    version = rootProject.property("neo_version") as String

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
    // Nested natives (same idea as Fabric Loom include).
    jarJar(implementation("de.maxhenkel.rnnoise4j:rnnoise4j:2.1.2")!!)
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to rootProject.property("minecraft_version"),
        "minecraft_version_range" to rootProject.property("minecraft_version_range"),
        "neo_version" to rootProject.property("neo_version"),
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
    options.release.set(25)
    options.encoding = "UTF-8"
}
