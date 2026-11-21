plugins {
    application
    alias(libs.plugins.javafx)
}

javafx {
    version = libs.versions.javafx.runtime.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("com.dau.labmanagement.client.ClientApp")
}

dependencies {
    implementation(project(":shared"))
    testImplementation(libs.junit.jupiter)
}
