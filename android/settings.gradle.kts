// Claims clerk app. Includes the two platform-stack Android modules as SOURCE subprojects
// (pointing into the vendor/platform-stack submodule) rather than published artifacts, with
// this build's unified plugin versions — so they compile against Kotlin 2.0 / AGP 8.5 here
// regardless of platform-stack's own standalone settings.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.5.0"
        id("com.android.library") version "8.5.0"
        id("org.jetbrains.kotlin.android") version "2.0.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    }
}

dependencyResolutionManagement {
    // Ignore the repositories the vendored modules declare; use these for everything.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "claims-android"

include(":app")

include(":android-auth")
project(":android-auth").projectDir = file("../vendor/platform-stack/android-auth")

include(":platform-login-ui")
project(":platform-login-ui").projectDir = file("../vendor/platform-stack/platform-login-ui")
