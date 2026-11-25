plugins {
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":shared"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
