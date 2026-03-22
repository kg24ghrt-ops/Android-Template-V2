pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("com.highcapable.sweetdependency") version "1.0.4"
    id("com.highcapable.sweetproperty") version "1.0.8"
}

sweetProperty {
    // Disable automation for all subprojects by default
    rootProject { 
        all { 
            isEnable = false 
        } 
    }
}

// Project name and modules
rootProject.name = "movie"
include(":app")