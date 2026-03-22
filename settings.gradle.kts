pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// Remove these lines completely
// plugins {
//     id("com.highcapable.sweetdependency") version "1.0.4"
//     id("com.highcapable.sweetproperty") version "1.0.8"
// }

// sweetProperty {
//     rootProject { 
//         all { 
//             isEnable = false 
//         } 
//     }
// }

rootProject.name = "movie"
include(":app")