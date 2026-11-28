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
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

application {
    mainClass.set("com.dau.labmanagement.server.ServerApp")
}

tasks.withType<Test>().configureEach {
    include("**/*Test.class", "**/*Tests.class", "**/*TestCase.class", "**/*IT.class")
}

tasks.register<JavaExec>("demoLogin") {
    group = "application"
    description = "Mở màn hình SC_001 với PostgreSQL nhúng và tài khoản demo"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.dau.labmanagement.server.testsupport.LoginDemoLauncher")
    jvmArgs("-Xmx512m", "-XX:MaxMetaspaceSize=192m", "-XX:+UseSerialGC")
}

tasks.register<JavaExec>("captureScreens") {
    group = "verification"
    description = "Xuất ảnh màn hình đăng nhập và dashboard vào build/screenshots"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.dau.labmanagement.server.testsupport.ScreenCaptureTool")
    jvmArgs("-Xmx512m", "-XX:MaxMetaspaceSize=192m", "-XX:+UseSerialGC")
}

tasks.register<JavaExec>("robotLogin") {
    group = "verification"
    description = "Chạy SC_001 bằng chuột/bàn phím thật (JavaFX Robot) và chụp ảnh từng bước"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.dau.labmanagement.server.testsupport.RobotLoginDemo")
    jvmArgs("-Xmx512m", "-XX:MaxMetaspaceSize=192m", "-XX:+UseSerialGC")
}
