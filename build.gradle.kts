import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType.ALL
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.Coroutines.ENABLE
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import us.kirchmeier.capsule.manifest.CapsuleManifest
import us.kirchmeier.capsule.spec.ReallyExecutableSpec
import us.kirchmeier.capsule.task.*
import kotlin.coroutines.experimental.*
import kotlinx.coroutines.experimental.*
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.internal.HasConvention
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet


buildscript {
    var javaVersion: JavaVersion by extra
    var kotlinVersion: String by extra
    var kotlinxVersion: String by extra
    var kotlinEAPRepo: String by extra
    var springBootVersion: String by extra

    kotlinVersion = "1.1.1"
    kotlinxVersion = "0.13"
    javaVersion = JavaVersion.VERSION_1_8
    springBootVersion = "2.0.0.BUILD-SNAPSHOT"
    kotlinEAPRepo = "http://dl.bintray.com/kotlin/kotlin-eap-1.1"

    repositories {
        gradleScriptKotlin()
        maven { setUrl(kotlinEAPRepo) }
        maven { setUrl("https://repo.spring.io/snapshot") }
        maven { setUrl("https://repo.spring.io/milestone") }
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")
        classpath("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
    }
}

printHeader()
val author by project
val javaVersion: JavaVersion by extra
val kotlinVersion: String by extra
val kotlinxVersion: String by extra
val springBootVersion: String by extra
val kotlinEAPRepo: String by extra
val moshiVersion = "1.4.0"
val jnrVersion = "3.0.37"

plugins {
    java
    application
    idea
    `help-tasks`
    id("org.jetbrains.kotlin.jvm") version "1.1.1"
    id("org.jetbrains.kotlin.kapt") version "1.1.1"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.1.1"
    id("org.jetbrains.kotlin.plugin.noarg") version "1.1.1"
    id("org.jetbrains.kotlin.plugin.spring") version "1.1.1"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.1.1"

    id("us.kirchmeier.capsule") version "1.0.2"
    id("com.dorongold.task-tree") version "1.3"

    id("org.springframework.boot") version "1.5.2.RELEASE" apply false
    id("org.jetbrains.kotlin.android") version "1.1.1" apply false
    id("org.jetbrains.kotlin.android.extensions") version "1.1.1" apply false
}

/**
 * Apply third party plugins.
 */
apply {
    plugin("org.springframework.boot")
}

base {
    group = "io.sureshg"
    version = "1.0"
    description = "Gradle script kotlin starter!"
}

/**
 * Configure java compiler options.
 */
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

/**
 * Configure application plugin
 */
application {
    applicationName = rootProject.name
    mainClassName = "io.sureshg.KotlinMainKt"
}

/**
 * Enable coroutines.
 */
kotlin {
    experimental.coroutines = ENABLE
}

/**
 * Enable java incremental compilation.
 */
tasks.withType<JavaCompile> {
    options.isIncremental = true
}

/**
 * Configure kotlin compiler options.
 */
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
}

repositories {
    gradleScriptKotlin()
    maven { setUrl(kotlinEAPRepo) }
    maven { setUrl("https://repo.spring.io/snapshot") }
    maven { setUrl("https://repo.spring.io/milestone") }
    mavenCentral()
}

dependencies {
    compile(kotlinModule("stdlib-jre8", kotlinVersion))
    compile(kotlinModule("reflect", kotlinVersion))
    compile(kotlinxModule("coroutines-core", kotlinxVersion))
    compile("com.squareup.moshi:moshi:$moshiVersion")
    compile("com.github.jnr:jnr-posix:$jnrVersion")
}


/**
 * Show source sets.
 */
val compileJava: JavaCompile by tasks
compileJava.doFirst {
    println("<====== Source Sets ======>")
    java().sourceSets.asMap.forEach { name, srcSet ->
        val ktSrcSet = (srcSet as HasConvention).convention.getPlugin<KotlinSourceSet>()
        println("Java-${name.capitalize()} => ${srcSet.allSource.srcDirs.map { it.name }}")
        println("Kotlin-${name.capitalize()} => ${ktSrcSet.kotlin.srcDirs.map { it.name }}")
    }
    println("<=========================>")
}

/**
 * Auto expand gradle properties.
 */
tasks.withType<ProcessResources> {
    filesMatching("application.yaml") {
        expand(project.properties)
    }
    onlyIf { file("src/main/resources/application.yaml").exists() }
}

/**
 * Make executable
 */
task<FatCapsule>("makeExecutable") {
    // val appConfig = project.convention.getPlugin(ApplicationPluginConvention::class)
    val minJavaVer = javaVersion.toString()
    archiveName = application().applicationName
    reallyExecutable = ReallyExecutableSpec().trampolining()
    capsuleManifest = CapsuleManifest().apply {
        premainClass = "Capsule"
        mainClass = "Capsule"
        applicationName = application().applicationName
        applicationClass = application().mainClassName
        applicationVersion = version
        jvmArgs = listOf("-client")
        args = listOf("$*")
        minJavaVersion = minJavaVer
    }
    dependsOn("clean")
}

/**
 * Generate Gradle Script Kotlin wrapper.
 */
task<Wrapper>("wrapper") {
    description = "Generate Gradle wrapper."
    //gradleVersion = "3.5"
    distributionType = ALL
    distributionUrl = getGskURL("3.5-20170305000422+0000")
}

/**
 * Set default task
 */
defaultTasks("clean", "tasks", "--all")

/**
 * A tasks using coroutines.
 */
task("fib") {
    description = "A fibonacci task using kotlin generators."
    doLast {
        fib().take(10).forEach(::println)
    }
}

task("async") {
    description = "An async task using kotlin coroutines."
    doLast {
        runBlocking {
            launch(CommonPool) {
                delay(2000)
                println("Gradle Script Kotlin!")
            }
            print("Hello, ")
            delay(2000)
        }
        println("By ${author ?: "Suresh"}")
    }
}

/**
 * Helper/extension functions.
 */
fun getGskURL(version: String, type: DistributionType = ALL) = "https://repo.gradle.org/gradle/dist-snapshots/gradle-script-kotlin-$version-${type.name.toLowerCase()}.zip"

fun fib() = buildSequence {
    var a = 0
    var b = 1
    while (true) {
        yield(b)
        val next = a + b
        a = b
        b = next
    }
}

fun printHeader() {
    val header = """
                 +-----------------------------+
                 | Kotlin Starter Build Script |
                 +-----------------------------+
                 """.trimIndent()
    println(header)
    println("\nConfigured project properties are,")
    extra.properties.entries.sortedBy { it.key }.forEach {
        println("%-18s = %-20s".format(it.key, it.value))
    }
    println()
}

fun DependencyHandler.kotlinxModule(module: String, version: String = kotlinxVersion) = "org.jetbrains.kotlinx:kotlinx-$module:$version"

