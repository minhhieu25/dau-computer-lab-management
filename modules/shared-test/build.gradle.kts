// Test fixtures: fake client/server used by integration tests
// before real UI or agent components exist.
plugins {
    `java-library`
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
