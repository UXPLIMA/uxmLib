plugins {
    id("uxmlib.java-conventions")
    id("uxmlib.publish-conventions")
}

dependencies {
    api(project(":uxmlib-common"))
    api(libs.hikari)
    api(libs.caffeine)

    // The SQLite driver is the default backend, so it ships as an api dependency. MariaDB/MySQL and
    // PostgreSQL are network options a consumer opts into; declared compileOnly here and added by the
    // consumer when they select that backend.
    api(libs.sqlite.jdbc)
    compileOnly(libs.mariadb.jdbc)
    compileOnly(libs.postgresql)

    // Storage is pure infra (no Paper). Tests run a real in-memory SQLite, so they are plain JUnit.
    testImplementation(libs.sqlite.jdbc)
}
