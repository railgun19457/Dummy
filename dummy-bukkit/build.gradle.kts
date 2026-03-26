plugins {
    id("java")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation(project(":dummy-common"))
    implementation(project(":dummy-core"))
    implementation(project(":dummy-nms:dummy-nms-api"))
    implementation(project(":dummy-nms:dummy-nms-v1_21_1", configuration = "reobf"))
}

tasks.jar {
    archiveBaseName.set("Dummy")
    archiveVersion.set(project.version.toString())

    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
