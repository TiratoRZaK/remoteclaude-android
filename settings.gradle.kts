import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "remoteclaude-android"

include(":protocol")

// Android SDK ищем в local.properties, затем в переменных окружения, затем по
// стандартным путям установки. Модуль :app подключается только когда SDK найден:
// на машине без SDK остаётся рабочей вся JVM-часть (./gradlew :protocol:test).
val sdkDir: File? = run {
    val localProperties = file("local.properties")
    if (localProperties.isFile) {
        val props = Properties()
        localProperties.inputStream().use(props::load)
        val declared = props.getProperty("sdk.dir")?.let(::File)
        if (declared != null && declared.isDirectory) return@run declared
    }
    val home = System.getProperty("user.home")
    listOfNotNull(
        System.getenv("ANDROID_HOME"),
        System.getenv("ANDROID_SDK_ROOT"),
        System.getenv("LOCALAPPDATA")?.let { "$it/Android/Sdk" },
        "$home/Android/Sdk",
        "$home/Library/Android/sdk",
    ).map(::File).firstOrNull { it.isDirectory }
}

if (sdkDir == null) {
    logger.lifecycle(
        "Android SDK не найден: модуль :app пропущен. Доступны JVM-задачи, например ./gradlew :protocol:test",
    )
} else {
    val localProperties = file("local.properties")
    if (!localProperties.isFile || !localProperties.readText().contains("sdk.dir")) {
        localProperties.writeText(
            "# Путь к Android SDK для AGP. Файл машинный, в git не попадает.\n" +
                "sdk.dir=${sdkDir.absolutePath.replace('\\', '/')}\n",
        )
    }
    include(":app")
}
