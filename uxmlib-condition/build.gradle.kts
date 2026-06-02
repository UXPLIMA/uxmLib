plugins {
    id("uxmlib.java-conventions")
    id("uxmlib.publish-conventions")
}

dependencies {
    api(project(":uxmlib-common")) // Text seam for rendering failure messages
    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.adventure)

    // The comparator and the placeholder condition are pure logic and unit-test as plain JUnit; MockBukkit
    // only smoke-tests the Player-bound wiring of a request against a real Paper server.
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api)
    testImplementation(libs.bundles.adventure)
    testImplementation(libs.bundles.testing)
}
