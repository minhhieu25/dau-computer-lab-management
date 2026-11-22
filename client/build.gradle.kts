plugins {
    application
    alias(libs.plugins.javafx)
}

javafx {
    version = libs.versions.javafx.runtime.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass = "com.dau.labmanagement.client.ClientApp"
}
