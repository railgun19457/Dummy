plugins {
    id("java-library")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.11.0")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}
