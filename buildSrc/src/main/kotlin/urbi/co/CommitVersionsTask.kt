package urbi.co

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

abstract class CommitVersionsTask: DefaultTask() {

    private var buildDir: String = project.buildDir.toString()

    init {
        buildDir = project.buildDir.toString().replace("/build","")
    }

    @TaskAction
    fun commitVersionLib() {
        val tpaylib = "tpaylib"
        val appId = System.getProperty("args")
        var mapChangelog: LinkedHashMap<String, String>;
        if(appId == "tpay")
            mapChangelog = Util.getChangelogTpayMap()
        else
            mapChangelog = Util.getChangelogMap()
        // Create map from versions in depend.gradle file
        val mapVersion: HashMap<String, String> = Util.createMapVersion(buildDir)
        val outputGit = ByteArrayOutputStream()

        // Read module from name Version on Gradle file
        val mapModuleName: HashMap<String, String> = Util.getVersionKeyFromModule()

        project.exec {
            commandLine("git", "status")
            standardOutput = outputGit
        }
        var haveToPushTpayLib = false
        outputGit.toString().split("\n").reversed().forEach { line ->
            line.replace("\\s".toRegex(), "").let { newLine ->
                if (newLine.startsWith("modified:") || newLine.startsWith("modificato:")) {
                    val italianClearLine = newLine.replace("modificato:".toRegex(), "")
                    val clearLine = italianClearLine.replace("modified:".toRegex(), "")
                    val arrayRow = clearLine.split("/")
                    if (arrayRow.isNotEmpty() && mapVersion.containsKey(mapModuleName[arrayRow[0]])) {
                        val key = arrayRow[0]
                        val tagVersionKey = mapVersion[mapModuleName[key]]
                        println("Analyze ${key} version ${tagVersionKey}..............")
                        ByteArrayOutputStream().use { os ->
                            project.exec {
                                commandLine("./gradlew", "$key:formatKotlin")
                                standardOutput = os
                            }
                            println(os.toString())
                            project.exec {
                                println("Adding $key's files to Git")
                                commandLine("git", "add", "$key/*")
                                standardOutput = os
                            }
                            println(os.toString())
                            project.exec {
                                println("Git commit $key version $tagVersionKey.......")
                                commandLine("git", "commit", "-m", "\"Version bump $key $tagVersionKey\"")
                                standardOutput = os
                            }
                            println(os.toString())
                            project.exec {
                                println("Git push $key version $tagVersionKey.......")
                                commandLine("git", "push")
                                standardOutput = os
                            }
                            println(os.toString())
                            project.exec {
                                println("Creating git tag $${mapChangelog[key]}$tagVersionKey.......")
                                commandLine("git", "tag", "${mapChangelog[key]}$tagVersionKey")
                                standardOutput = os
                            }
                            println(os.toString())
                            project.exec {
                                println("Pushing git tag $${mapChangelog[key]}$tagVersionKey.......")
                                commandLine("git", "push", "--tags")
                                standardOutput = os
                            }
                            println(os.toString())
                        }
                    } else if (arrayRow.isNotEmpty() && "changelog.md".contains(arrayRow[0])) {
                        haveToPushTpayLib = true
                    }
                }
            }
        }
    }
}
