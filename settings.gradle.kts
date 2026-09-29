pluginManagement {
    repositories {
        // 优先使用阿里云镜像
        maven {
            name = "Aliyun Public"
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            name = "Aliyun Gradle Plugin"
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        maven {
            // RetroFuturaGradle
            name = "GTNH Maven"
            url = uri("https://nexus.gtnewhorizons.com/repository/public/")
            mavenContent {
                includeGroup("com.gtnewhorizons")
                includeGroupByRegex("com\\.gtnewhorizons\\..+")
            }
        }
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("com.gtnewhorizons.gtnhsettingsconvention") version("2.0.20")
}