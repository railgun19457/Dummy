rootProject.name = "Dummy"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

include("dummy-api")
include("dummy-common")
include("dummy-core")
include("dummy-nms:dummy-nms-api")
include("dummy-nms:dummy-nms-v1_21_1")
include("dummy-bukkit")
