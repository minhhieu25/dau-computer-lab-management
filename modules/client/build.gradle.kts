// PC Client (student machine): tray + overlay + agent + small JavaFX UI.
plugins {
    java
    application
    alias(libs.plugins.javafx)
}

dependencies {
    implementation(project(":shared"))

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
    mainClass = "com.dau.labmanagement.client.ClientApp"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
