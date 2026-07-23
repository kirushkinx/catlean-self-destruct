import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.loom)
}

group = providers.gradleProperty("maven_group").get()
version = "${providers.gradleProperty("mod_version").get()}+${libs.versions.minecraft.get()}"

base {
    archivesName.set(providers.gradleProperty("archives_base_name").get())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft(libs.minecraft)

    api(libs.fabric.loader)
    api(libs.fabric.api)
    api(libs.fabric.kotlin)

    compileOnly(files(providers.gradleProperty("catlean_jar").get()))
}

tasks.processResources {
    val properties = mapOf(
        "version"            to project.version.toString(),
        "minecraft_version"  to libs.versions.minecraft.get(),
        "loader_version"     to libs.versions.loader.get(),
        "archives_base_name" to providers.gradleProperty("archives_base_name").get()
    )
    inputs.properties(properties)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}
