package urbi.co

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

abstract class UploadGitHubPackage: DefaultTask()  {

    @get:Input
    abstract val skipService: Property<Boolean>

    private var buildDir: String = project.buildDir.toString()

    private var avoidPublishTpay: Boolean = false

    init {
        skipService.convention(false)
        buildDir = project.buildDir.toString().replace("/build","")
        avoidPublishTpay = project.properties["avoidPublishTpay"].toString().toBoolean()
    }

    @TaskAction
    fun uploadUrbiPackages() {
        val appId = System.getProperty("args")
        if (!skipService.get()) {
            if (appId == "tpay")
                forcePullServiceFileAndCopyTPayLib()
            else
                forcePullServiceFileAndCopy()
        } else
            println("No service file is force pull and uploaded, you know what you are doing.......")

        var mapChangelog: LinkedHashMap<String, String>;
        if(appId == "tpay")
            mapChangelog = Util.getChangelogTpayMap()
        else
            mapChangelog = Util.getChangelogMap()

        // Create map from versions in depend.gradle file
        val mapVersion: HashMap<String, String> = Util.createMapVersion(buildDir)

        println("FFFF $avoidPublishTpay")
//         Read Changelog Utility on Gradle file
//        mapChangelog.forEach { map ->
//            writeChangelog(mapVersion, mapChangelog, map.key, "${map.key}/changelog.md", appId)
//        }

        println("UPLOAD TASK COMPLETED.......")
    }

    private fun forcePullServiceFileAndCopy() {
        println("Pull  urbi-services-providers-file")
        ByteArrayOutputStream().use { os ->
            val result = project.exec  {
                commandLine("git", "submodule", "update","--recursive","--remote")
                standardOutput = os
            }
            println(os.toString())
        }
        println("Force Copy service file from urbi-services-providers-file")
        ByteArrayOutputStream().use { os ->
            val result = project.exec {
                commandLine("./gradlew", "mobilitylib:copyServicesProvider","-Dargs=force")
                standardOutput = os
            }
            println(os.toString())
        }
    }

    private fun forcePullServiceFileAndCopyTPayLib() {
        println("Pull  urbi-services-providers-file")
        ByteArrayOutputStream().use { os ->
            val result = project.exec {
                commandLine("git", "submodule", "update","--recursive","--remote")
                standardOutput = os
            }
            println(os.toString())
        }
        println("Force Copy service file from urbi-services-providers-file")
        ByteArrayOutputStream().use { os ->
            val result = project.exec {
                commandLine("./gradlew", "tpaylib:copyServicesProvider","-Dargs=force")
                standardOutput = os
            }
            println(os.toString())
        }
    }

    fun writeChangelog(
        mapVersion: HashMap<String, String>,
        mapChangelog: HashMap<String, String>,
        key: String,
        pathFile: String,
        appId: String? = null
    ) {
        println("Reading Changelog $pathFile........")
        val format = SimpleDateFormat("yyyy-MM-dd")
        val dataNow = format.format(Date())
        val changelog = when {
            File(pathFile).exists()-> File(pathFile)
            File("$buildDir/$pathFile").exists()-> File("$buildDir/$pathFile")
            else -> File("$buildDir/android-urbi-framework/$pathFile")
        }
        var lastIsUnrelase = false
        var haveToWriteFile = false
        val newChangelog = arrayListOf<String>()
        // Read module from name Version on Gradle file
        val mapModuleName: HashMap<String, String> = Util.getVersionKeyFromModule()
        val tagVersionKey = mapVersion[mapModuleName[key]]
        changelog.readLines().forEach { line ->
            if (line.startsWith("## [Unreleased]", true)) {
                lastIsUnrelase = true
                newChangelog.add(line)
            } else if (lastIsUnrelase && line.startsWith("##")) {
                return
            } else if (lastIsUnrelase && line.startsWith("-")) {
                newChangelog.add("## [${mapChangelog[key]}${tagVersionKey}] $dataNow")
                newChangelog.add(line)
                lastIsUnrelase = false
                haveToWriteFile = true
            } else
                newChangelog.add(line)
        }
        if (haveToWriteFile) {
            println("Update lib $key..........")
            if(avoidPublishTpay){
                ByteArrayOutputStream().use { os ->
                    val result = project.exec {
                        commandLine(
                            "./gradlew",
                            "$key:clean",
                            "$key:publishReleasePublicationToGitHubPackagesRepository",
                        )
                        standardOutput = os
                    }
                    println(os.toString())
                }
            }
            else if(appId != "tpay") {
                ByteArrayOutputStream().use { os ->
                    val result = project.exec {
                        commandLine(
                            "./gradlew",
                            "$key:clean",
                            "$key:publishReleasePublicationToGitHubPackagesRepository",
                            "$key:publishReleasePublicationToGitHubPackages2Repository"
                        )
                        standardOutput = os
                    }
                    println(os.toString())
                }
            } else {
                ByteArrayOutputStream().use { os ->
                    val result = project.exec {
                        commandLine(
                            "./gradlew",
                            "$key:clean",
                            "$key:publishReleasePublicationToGitHubPackagesRepository",
                        )
                        standardOutput = os
                    }
                    println(os.toString())
                }
            }
            if(Util.haveModuleThird(key))
                ByteArrayOutputStream().use { os ->
                    val result = project.exec {
                        commandLine("./gradlew", "$key:publishThirdPublicationToGitHubPackages-ThirdRepository")
                        standardOutput = os
                    }
                    println(os.toString())
                }
            println("Updating Changelog $pathFile..........")
            changelog.printWriter().use { out ->
                newChangelog.forEach {
                    out.println(it)
                }
            }
        }
    }
}
