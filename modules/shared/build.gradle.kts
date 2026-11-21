// Shared library: protocol, message types, DTOs, constants.
// Single source of truth — server and client must not re-declare these.
plugins {
    `java-library`
}

dependencies {
    // DTOs are Jackson records; expose Jackson to dependents via api.
    api(libs.jackson.databind)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
