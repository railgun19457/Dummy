plugins {
    id("java-library")
}

dependencies {
    api(project(":dummy-api"))
    api(project(":dummy-common"))
    implementation("com.google.code.gson:gson:2.11.0")
}
