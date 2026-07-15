plugins {
    id("dev.kikugie.stonecutter")
}

// Stonecutter controller (coexistence with include("fabric"|"neoforge"|"forge"|…)).
// Per-cell props live in versions/<mc>/<loader>/gradle.properties.
// Release targets: scripts/matrix-targets.json
//
// Full migrate (replace include(...) with Stonecutter trees):
// settings.gradle.kts:
//   plugins { id("dev.kikugie.stonecutter") version "0.9.6" }
//   stonecutter {
//     kotlinController = true
//     centralScript = "build.gradle.kts"
//     create(rootProject) {
//       fun mc(version: String, vararg loaders: String) {
//         loaders.forEach { loader ->
//           val v = version("$version-$loader", version)
//           v.buildscript = when {
//             loader == "fabric" && version.startsWith("26") -> "build.fabric.gradle.kts"
//             loader == "fabric" -> "build.fabric-legacy.gradle.kts"
//             loader == "neoforge" -> "build.neoforge.gradle.kts"
//             loader == "forge" -> "build.forge.gradle.kts"
//             else -> error(loader)
//           }
//         }
//       }
//       mc("1.18.2", "fabric", "forge")
//       mc("1.19.2", "fabric", "forge")
//       mc("1.19.4", "fabric", "forge")
//       mc("1.20.1", "fabric", "forge")
//       mc("1.20.4", "fabric", "neoforge")
//       mc("1.21.1", "fabric", "neoforge")
//       mc("1.21.4", "fabric", "neoforge")
//       mc("26.1.2", "fabric", "neoforge")
//       vcsVersion = "26.1.2-fabric"
//     }
//   }
// Then: stonecutter active "26.1.2-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    constants.match(
        current.project.substringAfterLast('-'),
        "fabric",
        "neoforge",
        "forge"
    )

    swaps["mod_version"] = "\"${property("mod_version")}\";"
    swaps["minecraft"] = "\"${current.version}\";"

    constants["custom_payload"] = eval(current.version, ">=1.20.5")
    constants["key_category_object"] = eval(current.version, ">=1.21.9")
    constants["header_footer_layout"] = eval(current.version, ">=1.20.2")
    constants["identifier_rename"] = eval(current.version, ">=1.21")
    constants["gui_graphics_extractor"] = eval(current.version, ">=26.1")
}
