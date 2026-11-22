plugins {
    application
    alias(libs.plugins.javafx)
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.jdbi3.core)
    implementation(libs.jdbi3.sqlobject)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    runtimeOnly(libs.postgresql)

    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.zonky.embedded.postgres)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

javafx {
    version = libs.versions.javafx.runtime.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass = "com.dau.labmanagement.server.ServerApp"
}

tasks.withType<Test>().configureEach {
    include("**/*Test.class", "**/*Tests.class", "**/*TestCase.class", "**/*IT.class")
}
