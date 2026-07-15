// Root aggregator — versions/<minecraft>/<loader>/
// Build: ./gradlew buildAll
// Release targets: scripts/matrix-targets.json

tasks.register("buildAll") {
    dependsOn(
        ":26.1.2-fabric:jar",
        ":26.1.2-neoforge:jar",
        ":1.21.4-fabric:remapJar",
        ":1.21.4-neoforge:jar",
        ":1.21.1-fabric:remapJar",
        ":1.21.1-neoforge:jar",
        ":1.20.4-fabric:remapJar",
        ":1.20.4-neoforge:remapJar",
        ":1.20.1-fabric:remapJar",
        ":1.20.1-forge:remapJar",
        ":1.19.4-fabric:remapJar",
        ":1.19.4-forge:remapJar",
        ":1.19.2-fabric:remapJar",
        ":1.19.2-forge:remapJar",
        ":1.18.2-fabric:remapJar",
        ":1.18.2-forge:remapJar",
    )
    group = "build"
    description = "Build Mutualzz Voice jars for all ready MC×loader cells"
}

tasks.register("buildMatrix") {
    group = "build"
    description = "Alias for buildAll (see scripts/matrix-targets.json)"
    dependsOn("buildAll")
}
