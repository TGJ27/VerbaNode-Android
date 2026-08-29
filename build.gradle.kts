plugins {
    id("base")
    id("com.android.application") version "9.3.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}


// Root lifecycle clean used by the Windows build helpers.
// Gradle's base plugin creates :clean; include the Android module output
// so `gradlew clean` reliably resets the whole single-module project.
tasks.named<Delete>("clean") {
    delete(layout.buildDirectory)
    delete(file("app/build"))
}
