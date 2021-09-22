import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.*

tasks.register("uploadlib") {
    doLast {
        val appId = System.getProperty("args")

        var mapChangelog: LinkedHashMap<String, String>;
        if(appId == "tpay")
            mapChangelog = getChangelogTpayMap()
        else
            mapChangelog = getChangelogMap()

        // Read Version on Gradle file
        val mapVersion: HashMap<String, String> = createMapVersion()
        println("Start to scan Changelog files.......")

////         Read Changelog Utility on Gradle file
        mapChangelog.forEach { map ->
            writeChangelog(mapVersion, mapChangelog, map.key, "${map.key}/changelog.md")
        }

        println("UPLOAD TASK COMPLETED.......")

    }
}
tasks.register("commitversions") {
    doLast {
        val tpaylib = "tpaylib"
        val appId = System.getProperty("args")
        var mapChangelog: LinkedHashMap<String, String>;
        if(appId == "tpay")
            mapChangelog = getChangelogTpayMap()
        else
            mapChangelog = getChangelogMap()
        // Read Version on Gradle file
        val mapVersion: HashMap<String, String> = createMapVersion()
        // Read Version on Gradle file
        val outputGit = ByteArrayOutputStream()
        exec {
            commandLine("git", "status")
            standardOutput = outputGit
        }
        var haveToPushTpayLib = false
        outputGit.toString().split("\n").reversed().forEach { line ->
            line.replace("\\s".toRegex(), "").let { newLine ->
                if (newLine.startsWith("modified:")) {
                    val clearLine = newLine.replace("modified:".toRegex(), "")
                    val arrayRow = clearLine.split("/")
                    if (arrayRow.isNotEmpty() && mapVersion.containsKey(arrayRow[0])) {
                        val key = arrayRow[0]
                        val version = mapVersion[key]
                        println("Analyze ${key} ${version}..............")
                        ByteArrayOutputStream().use { os ->
                            exec {
                                commandLine("./gradlew", "$key::formatKotlin")
                                standardOutput = os
                            }
                            println(os.toString())
                            exec {

                                println("Adding $key's files to Git")
                                commandLine("git", "add", "$key/*")
                                standardOutput = os
                            }
                            println(os.toString())
                            exec {
                                println("Git commit $key $version.......")
                                commandLine("git", "commit", "-m", "\"Version bump $key $version\"")
                                standardOutput = os
                            }
                            println(os.toString())
                            exec {
                                println("Git push $key $version.......")
                                commandLine("git", "push")
                                standardOutput = os
                            }
                            println(os.toString())
                            exec {
                                println("Git tag $${mapChangelog[key]} $version.......")
                                commandLine("git", "tag", "${mapChangelog[key]}$version")
                                standardOutput = os
                            }
                            println(os.toString())
                            exec {
                                println("Git push tags $${mapChangelog[key]} $version.......")
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

fun getChangelogMap(): LinkedHashMap<String, String> = linkedMapOf(
    "utilitylib" to "utilitylib_",
    "urbimodel" to "URM_",
    "urbicore" to "URC_",
    "designsystem" to "DESL_",
    "urbiscan" to "URS_",
    "urbisearch" to "search_",
    "urbipay" to "urbipay_",
    "ticketlib" to "ticketlib_",
    "urbitaxi" to "urbitaxi_",
    "evcharging" to "EVC_",
    "transpo" to "TRA_",
    "tripo" to "Tripo_",
    "mobilitylib" to "ML_",
)

fun getChangelogTpayMap(): LinkedHashMap<String, String> = linkedMapOf(
    "telepasspaymodel" to "telepasspaymodel_",
    "telepasspaynetwork" to "telepasspaynetwork_",
    "tpaylib" to "TPL_"
)

fun createMapVersion(): HashMap<String, String> {
    val tpaylib = "tpaylib"
    // Read Version on Gradle file
    val mapVersion: HashMap<String, String> = hashMapOf()
    val gradle = File("android-scripts/gradle/depend.gradle")
    var readVersion = false
    println("Reading /gradle/depend.gradle file for version ......")
    gradle.forEachLine { line ->
        if (line.equals("//---End---//", true)) {
            readVersion = false
        }
        if (readVersion) {
            line.replace("\\s".toRegex(), "").let { lineW ->
                lineW.split("=").let {
                    if (it[0].equals(tpaylib, true)) {
                        mapVersion[it[0]] =
                            it[1].replace("\'".toRegex(), "").replace("\\+".toRegex(), "").replace(
                                "tpaylib_code".toRegex(), mapVersion["tpaylib_code"]
                                    ?: ""
                            )
                    } else mapVersion[it[0]] = it[1].replace("\'".toRegex(), "")

                }
            }
        }
        if (line.equals("//---Module Version---//", true))
            readVersion = true
    }
    return mapVersion
}

fun writeChangelog(
    mapVersion: HashMap<String, String>,
    mapChangelog: HashMap<String, String>,
    key: String,
    pathFile: String
) {
    println("Reading Changelog $pathFile........")
    val format = SimpleDateFormat("yyyy-MM-dd")
    val dataNow = format.format(Date())
    val changelog = File(pathFile)
    var lastIsUnrelase = false
    var haveToWriteFile = false
    val newChangelog = arrayListOf<String>()
    changelog.readLines().forEach { line ->
        if (line.startsWith("## [Unreleased]", true)) {
            lastIsUnrelase = true
            newChangelog.add(line)
        } else if (lastIsUnrelase && line.startsWith("##")) {
            return
        } else if (lastIsUnrelase && line.startsWith("-")) {
            newChangelog.add("## [${mapChangelog[key]}${mapVersion[key]}] $dataNow")
            newChangelog.add(line)
            lastIsUnrelase = false
            haveToWriteFile = true
        } else
            newChangelog.add(line)
    }
    if (haveToWriteFile) {
        println("Update lib $key..........")
        ByteArrayOutputStream().use { os ->
            val result = exec {
                commandLine("./gradlew", "$key:clean", "$key:publish")
                standardOutput = os
            }
            println(os.toString())
        }
        println("Updating Changelog $pathFile..........")
        File(pathFile).printWriter().use { out ->
            newChangelog.forEach {
                out.println(it)
            }
        }
    }
}