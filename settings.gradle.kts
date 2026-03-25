pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // This ensures all modules (like :app) use these central repositories
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        
        // 🔥 MOZILLA REPOSITORY - Required for GeckoView
        maven { 
            url = uri("https://maven.mozilla.org/maven2") 
        }
    }
}

rootProject.name = "movie"
include(":app")
