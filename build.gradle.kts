plugins {
    alias(libs.plugins.javafx) apply false
}

allprojects {
    group = "com.dau.labmanagement"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
        toolchain.languageVersion.set(
            org.gradle.jvm.toolchain.JavaLanguageVersion.of(21)
        )
    }

    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        useJUnitPlatform()
    }
}
