plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    implementation(project(":dummy-nms:dummy-nms-api"))
}
