plugins {
    id("java-library")
}

dependencies {
    api(project(":dummy-api"))
    implementation(project(":dummy-common"))
    implementation(project(":dummy-nms:dummy-nms-api"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.16.1")
    testImplementation("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

tasks.test {
    useJUnitPlatform()
}
