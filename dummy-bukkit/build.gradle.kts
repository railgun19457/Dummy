plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    implementation(project(":dummy-common"))
    implementation(project(":dummy-core"))
    implementation(project(":dummy-nms:dummy-nms-api"))
    implementation(project(":dummy-nms:dummy-nms-v1_21_1"))
}

tasks.jar {
    archiveBaseName.set("Dummy")
    archiveVersion.set(project.version.toString())
}
