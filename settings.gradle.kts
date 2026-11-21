// Root settings — multi-module Gradle build.
// Structure follows Docs/ke_hoach_trien_khai.md, section 3.
rootProject.name = "dau-computer-lab-management"

include("shared")
include("server")
include("client")
include("shared-test")

project(":shared").projectDir = file("modules/shared")
project(":server").projectDir = file("modules/server")
project(":client").projectDir = file("modules/client")
project(":shared-test").projectDir = file("modules/shared-test")

// Single repository definition for every module.
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
