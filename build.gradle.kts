plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    kotlin("jvm") version "2.0.20"
    application
}

repositories { mavenCentral() }

kotlin { jvmToolchain(17) }

application { mainClass.set("Render_cursorKt") }

sourceSets.named("main") {
    kotlin.srcDir(".")
    kotlin.include("render-cursor.kt")
}

tasks.register("renderCursor") {
    group = "tools"
    description = "Render buddycursor_icon.png from render-cursor.kt"
    dependsOn(tasks.named("run"))
}
