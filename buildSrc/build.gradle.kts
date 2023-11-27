import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.*

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main'
    // that automatically become available as plugins in the main build.
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
    mavenCentral()
}
dependencies {
    // buildSrc in combination with this plugin ensures that the version set here
    // will be set to the same for all other Kotlin dependencies / plugins in the project.
    add("implementation", libs.findLibrary("google-gson").get())
}


//tasks.register("print-changelog-changed") {
//    doLast {
//        var mapChangelog = getChangelogMap()
//
//        // Create map from versions in depend.gradle file
//        val mapVersion: HashMap<String, String> = createMapVersion()
//
//        println("Start to scan Changelog files.......")
//
//////         Read Changelog Utility on Gradle file
//        mapChangelog.forEach { map ->
//            printChangelogChanged(mapVersion, mapChangelog, map.key, "${map.key}/changelog.md")
//        }
//
//        println("TASK COMPLETED.......")
//
//    }
//}
//
//
//
//fun printChangelogChanged(
//    mapVersion: HashMap<String, String>,
//    mapChangelog: HashMap<String, String>,
//    key: String,
//    pathFile: String
//) {
//    val format = SimpleDateFormat("yyyy-MM-dd")
//    val dataNow = format.format(Date())
//    val changelog = if(File(pathFile).exists()) File(pathFile) else File("$rootDir/android-urbi-framework/$pathFile")
//    var lastIsUnrelase = false
//    var haveToWriteFile = false
//    // Read module from name Version on Gradle file
//    val mapModuleName: HashMap<String, String> = getVersionKeyFromModule()
//    val tagVersionKey = mapVersion[mapModuleName[key]]
//    changelog.readLines().forEach { line ->
//        if (line.startsWith("## [Unreleased]", true)) {
//            lastIsUnrelase = true
//        } else if (lastIsUnrelase && line.startsWith("##")) {
//            return
//        } else if (lastIsUnrelase && line.startsWith("-")) {
//            lastIsUnrelase = false
//            haveToWriteFile = true
//        }
//    }
//    if (haveToWriteFile) {
//        println("Changelod changed  $key..........")
//    }
//}
//
//

fun <K, V> Map<K, V>.inverseMap() = map { Pair(it.value, it.key) }.toMap()
