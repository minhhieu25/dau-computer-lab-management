plugins {
    alias(libs.plugins.javafx)
}

javafx {
    version = libs.versions.javafx.runtime.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(project(":shared"))
    testImplementation(libs.junit.jupiter)
}
