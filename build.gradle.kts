plugins {
    id("com.android.application") version "8.11.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.3")
    }
}