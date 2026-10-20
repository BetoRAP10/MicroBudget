pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MicroBudget"

include(":core:domain")   // Lógica de negocio pura (Kotlin/JVM): presupuesto, alertas, estadísticas, sync
include(":core:data")     // Android: Room, repositorios, notificaciones, comunicación reloj <-> celular
include(":wear")          // App Wear OS (Jetpack Compose for Wear OS)
include(":mobile")        // App Android complementaria (Jetpack Compose + Material 3)
