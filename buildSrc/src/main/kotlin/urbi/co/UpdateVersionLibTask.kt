package urbi.co

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import urbi.co.Util.inverseMap
import java.io.ByteArrayOutputStream
import java.io.File

abstract class UpdateVersionLibTask : DefaultTask() {


    private var buildDir: String = project.buildDir.toString()

    init {
        buildDir = project.buildDir.toString().replace("/build","")
    }

    @TaskAction
    fun updateVersion() {
        val mapVersionUrbi = Util.getVersionKeyFromModule()
        val mapVersionUrbiInverse = mapVersionUrbi.inverseMap()
        val keyToChangeVersion: HashSet<String> = HashSet()
        println("Start to scan Changelog files.......")
////         Read Changelog Utility on Gradle file
        mapVersionUrbi.forEach mapFor@{ map ->
            var lastIsUnrelase = false
            val pathFile = "${map.key}/changelog.md"
            try {
                val changelog =
                    if (File(pathFile).exists()) File(pathFile) else File("${buildDir}/android-urbi-framework/$pathFile")
                changelog.readLines().forEach { line ->
                    if (line.startsWith("## [Unreleased]", true)) {
                        lastIsUnrelase = true
                    } else if (lastIsUnrelase && line.startsWith("##")) {
                        return@mapFor
                    } else if (lastIsUnrelase && line.startsWith("-")) {
                        lastIsUnrelase = false
                        keyToChangeVersion.add(map.key)
                    }
                }
            } catch (e: Exception) {
                println("Error for file $pathFile ${e.message}")
            }
        }
        if (keyToChangeVersion.isNotEmpty()) {
            val newGradleDeep = arrayListOf<String>()
            val gradle =
                if (File("android-scripts/gradle/depend.gradle").exists()) File("android-scripts/gradle/depend.gradle") else File(
                    "${buildDir}/android-urbi-framework/android-scripts/gradle/depend.gradle"
                )
            var readVersion = false
            println("Reading /gradle/depend.gradle file for version......")
            gradle.forEachLine { line ->
                if (line.equals("//---End---//", true)) {
                    readVersion = false
                    newGradleDeep.add(line)
                } else if (readVersion) {
                    line.replace("\\s".toRegex(), "").let { lineW ->
                        lineW.split("=").let { it ->
                            if (it.size > 1 && mapVersionUrbiInverse.containsKey(it[0]) && keyToChangeVersion.contains(
                                    mapVersionUrbiInverse[it[0]]
                                )
                            ) {
                                val versionArray = it[1].replace("\'".toRegex(), "").split(".")
                                var mirrorVersion = versionArray[2]
                                var extraMirrorVersion = ""
                                if (!mirrorVersion.all { it.isDigit() }) {
                                    val arrMirror = mirrorVersion.split("-")
                                    mirrorVersion = arrMirror[0]
                                    extraMirrorVersion = arrMirror[1]
                                }
                                mirrorVersion = "${mirrorVersion.toInt() + 1}"
                                var newVersionApp = "${versionArray[0]}.${versionArray[1]}.${mirrorVersion}"
                                if (extraMirrorVersion.isNotEmpty())
                                    newVersionApp = "$newVersionApp-$extraMirrorVersion"
                                newGradleDeep.add(line.replace(it[1].replace("\'".toRegex(), ""), newVersionApp))
                            } else
                                newGradleDeep.add(line)
                        }
                    }
                } else if (line.equals("//---Module Version---//", true)) {
                    readVersion = true
                    newGradleDeep.add(line)
                } else
                    newGradleDeep.add(line)
            }
            println("Update dep file")
            gradle.printWriter().use { out ->
                newGradleDeep.forEach {
                    out.println(it)
                }
            }
            println("Upload Libs............")
            ByteArrayOutputStream().use { os ->
                val result = project.exec {
                    commandLine("./gradlew", "uploadlib", "-Pargs=skipService")
                    standardOutput = os
                }
                println(os.toString())
                println("Upload Libs RESULT$result")
            }
        } else
            println("No Version have updated")

    }
}
