// Root build file. Declares plugin versions once here (apply false = "make
// available, don't apply to this project") so the app module below can
// apply them without repeating version numbers.
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

// This project lives inside a OneDrive-synced folder. OneDrive actively
// scans/locks files as they change, and Gradle's build/ directory churns
// through thousands of small files on every build — the two fight over
// file handles and builds intermittently fail with AccessDenied/"Unable
// to delete directory" errors. Build output is disposable (already
// gitignored) and has no reason to be cloud-synced anyway, so it's
// redirected to a plain local folder outside OneDrive. user.home is used
// instead of a hardcoded drive letter so this works on any teammate's
// machine, on any OS.
val externalBuildDir = File(System.getProperty("user.home"), ".gradle-builds/smsms-android")
layout.buildDirectory.set(externalBuildDir.resolve(name))
subprojects {
    layout.buildDirectory.set(externalBuildDir.resolve(name))
}
