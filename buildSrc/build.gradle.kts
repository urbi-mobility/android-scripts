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
///**
// * This scripts read depend.gradle file where libs version are and update that if there is some change in their changelog
// */
//tasks.register("update-version-lib") {
//    doLast {
//        val mapVersionUrbi = getVersionKeyFromModule()
//        val mapVersionUrbiInverse = mapVersionUrbi.inverseMap()
//        val keyToChangeVersion: HashSet<String> = HashSet()
//        println("Start to scan Changelog files.......")
//////         Read Changelog Utility on Gradle file
//        mapVersionUrbi.forEach mapFor@{  map ->
//            var lastIsUnrelase = false
//            val pathFile = "${map.key}/changelog.md"
//            try {
//                val changelog =
//                    if (File(pathFile).exists()) File(pathFile) else File("$rootDir/android-urbi-framework/$pathFile")
//                changelog.readLines().forEach { line ->
//                    if (line.startsWith("## [Unreleased]", true)) {
//                        lastIsUnrelase = true
//                    } else if (lastIsUnrelase && line.startsWith("##")) {
//                        return@mapFor
//                    } else if (lastIsUnrelase && line.startsWith("-")) {
//                        lastIsUnrelase = false
//                        keyToChangeVersion.add(map.key)
//                    }
//                }
//            }catch (e: Exception){
//                println("Error for file $pathFile ${e.message}")
//            }
//        }
//        if(keyToChangeVersion.isNotEmpty()){
//            val newGradleDeep = arrayListOf<String>()
//            val gradle = if(File("android-scripts/gradle/depend.gradle").exists()) File("android-scripts/gradle/depend.gradle") else File("$rootDir/android-urbi-framework/android-scripts/gradle/depend.gradle")
//            var readVersion = false
//            println("Reading /gradle/depend.gradle file for version......")
//            gradle.forEachLine { line ->
//                if (line.equals("//---End---//", true)) {
//                    readVersion = false
//                    newGradleDeep.add(line)
//                }
//                else if (readVersion) {
//                    line.replace("\\s".toRegex(), "").let { lineW ->
//                        lineW.split("=").let { it ->
//                            if(it.size > 1 && mapVersionUrbiInverse.containsKey(it[0]) && keyToChangeVersion.contains(mapVersionUrbiInverse[it[0]])) {
//                                val versionArray = it[1].replace("\'".toRegex(), "").split(".")
//                                var mirrorVersion = versionArray[2]
//                                var extraMirrorVersion = ""
//                                if(!mirrorVersion.all { it.isDigit() } ){
//                                    val arrMirror= mirrorVersion.split("-")
//                                    mirrorVersion = arrMirror[0]
//                                    extraMirrorVersion = arrMirror[1]
//                                }
//                                mirrorVersion = "${mirrorVersion.toInt()+1}"
//                                var newVersionApp = "${versionArray[0]}.${versionArray[1]}.${mirrorVersion}"
//                                if(extraMirrorVersion.isNotEmpty())
//                                    newVersionApp = "$newVersionApp-$extraMirrorVersion"
//                                newGradleDeep.add(line.replace(it[1].replace("\'".toRegex(), ""),newVersionApp))
//                            }
//                            else
//                                newGradleDeep.add(line)
//                        }
//                    }
//                }
//                else if (line.equals("//---Module Version---//", true)) {
//                    readVersion = true
//                    newGradleDeep.add(line)
//                }
//                else
//                    newGradleDeep.add(line)
//            }
//            println("Update dep file")
//            gradle.printWriter().use { out ->
//                newGradleDeep.forEach {
//                    out.println(it)
//                }
//            }
//            println("Upload Libs............")
//            ByteArrayOutputStream().use { os ->
//                val result = exec {
//                    commandLine("./gradlew", "uploadlib", "-Pargs=skipService")
//                    standardOutput = os
//                }
//                println(os.toString())
//                println("Upload Libs RESULT$result")
//            }
//        }
//        else
//            println("No Version have updated")
//    }
//}

fun <K, V> Map<K, V>.inverseMap() = map { Pair(it.value, it.key) }.toMap()
