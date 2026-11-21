plugins {
    alias(libs.plugins.javafx)
}

javafx {
    version = libs.versions.javafx.runtime.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(project(":shared"))

    // JUnit 5
    testImplementation(libs.junit.jupiter)

    // PostgreSQL JDBC
    testImplementation("org.postgresql:postgresql:42.7.7")

    // Zonky Embedded PostgreSQL
    testImplementation("io.zonky.test:embedded-postgres:2.1.0")
}
