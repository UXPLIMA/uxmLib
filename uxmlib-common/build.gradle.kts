plugins {
    id("uxmlib.java-conventions")
    id("uxmlib.publish-conventions")
}

// Configurate 4.1.2 declares a strict dependency on geantyref 1.3.13, whose record reflection is broken
// (int record components map to 0). Force the compatible 1.3.16 across every configuration so record-based
// config mapping works at runtime and in tests. See gradle/libs.versions.toml for the rationale.
configurations.all {
    resolutionStrategy.force(libs.geantyref)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.adventure) // Paper ships Adventure at runtime
    api(libs.configurate.hocon)
    api(libs.geantyref) // pulled in by Configurate, surfaced so consumers also get the fixed version

    // The Bukkit-facing seams (Scheduler, Text) need Paper + Adventure on the test runtime,
    // since the production set declares them compileOnly.
    testImplementation(libs.paper.api)
    testImplementation(libs.bundles.adventure)
}
