// Root settings — multi-module Gradle build.
// Structure follows Docs/ke_hoach_trien_khai.md, section 3.
rootProject.name = "dau-computer-lab-management"

include("shared")
include("server")
include("client")
include("shared-test")

// Single repository definition for every module.
dependencyResolutionManagement {
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "dau-computer-lab-management"

include("shared", "server", "client", "shared-test")

