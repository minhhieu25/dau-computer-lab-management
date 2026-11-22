// PC Server (teacher/admin desktop app): JavaFX UI + services + DAO + network.
plugins {
    java
    application
    alias(libs.plugins.javafx)
}

dependencies {
    implementation(project(":shared"))

    // Database stack: pool (HikariCP) + query (JDBI 3) + migration (Flyway)
    implementation(libs.jdbi3.core)
    implementation(libs.jdbi3.sqlobject)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    runtimeOnly(libs.postgresql)

    // Embedded PostgreSQL for migration/integration tests (no Docker needed).
    testImplementation(libs.zonky.embedded.postgres)

    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

javafx {
    version = libs.versions.javafx.get()
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass = "com.dau.labmanagement.server.ServerApp"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Run from the repo root so "filesystem:db/migration" resolves to the shared migrations.
    workingDir = rootProject.projectDir
}

// Same reason as above for JavaExec (run / test... gradle tasks).
tasks.withType<JavaExec>().configureEach {
    workingDir = rootProject.projectDir
}
