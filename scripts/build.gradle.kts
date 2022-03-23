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

        // Create map from versions in depend.gradle file
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
        // Create map from versions in depend.gradle file
        val mapVersion: HashMap<String, String> = createMapVersion()
        val outputGit = ByteArrayOutputStream()

        // Read module from name Version on Gradle file
        val mapModuleName: HashMap<String, String> = getVersionKeyFromModule()

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
                    if (arrayRow.isNotEmpty() && mapVersion.containsKey(mapModuleName[arrayRow[0]])) {
                        val key = arrayRow[0]
                        val tagVersionKey = mapVersion[mapModuleName[key]]
                        println("Analyze ${key} version ${tagVersionKey}..............")
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
                                println("Git commit $key version $tagVersionKey.......")
                                commandLine("git", "commit", "-m", "\"Version bump $key $tagVersionKey\"")
                                standardOutput = os
                            }
                            println(os.toString())
                            exec {
                                println("Git push $key version $tagVersionKey.......")
                                commandLine("git", "push")
                                standardOutput = os
                            }
                            println(os.toString())
                            exec {
                                println("Creating git tag $${mapChangelog[key]}$tagVersionKey.......")
                                commandLine("git", "tag", "${mapChangelog[key]}$tagVersionKey")
                                standardOutput = os
                            }
                            println(os.toString())
                            exec {
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

fun getChangelogMap(): LinkedHashMap<String, String> = linkedMapOf(
    "utilitylib" to "UTL_",
    "urbimodel" to "MDL_",
    "urbicore" to "CRE_",
    "designsystem" to "DSG_",
    "urbiscan" to "SCN_",
    "urbisearch" to "SRC_",
    "urbipay" to "PAY_",
    "ticketlib" to "TCK_",
    "urbitaxi" to "TXI_",
    "evcharging" to "EVC_",
    "transpo" to "TRN_",
    "tripo" to "TRP_",
    "mobilitylib" to "MBL_",
)

fun getChangelogTpayMap(): LinkedHashMap<String, String> = linkedMapOf(
    "telepasspaymodel" to "TPM_",
    "telepasspaynetwork" to "TPN_",
    "tpaylib" to "TPL_"
)

fun getVersionKeyFromModule(): LinkedHashMap<String, String> = linkedMapOf(
    "utilitylib" to "utilityVersion",
    "urbimodel" to "modelVersion",
    "urbicore" to "coreVersion",
    "designsystem" to "designsystemVersion",
    "urbiscan" to "scanVersion",
    "urbisearch" to "searchVersion",
    "urbipay" to "payVersion",
    "ticketlib" to "ticketVersion",
    "urbitaxi" to "taxiVersion",
    "evcharging" to "evchargingVersion",
    "transpo" to "transpoVersion",
    "tripo" to "tripoVersion",
    "mobilitylib" to "mobilitySharingVersion",
    "telepasspaymodel" to "telepassModelVersion",
    "telepasspaynetwork" to "telepassNetworkVersion",
    "tpaylib" to "telepassLibVersion",
)

fun createMapVersion(): HashMap<String, String> {
    val tpaylib = "telepassLibVersion"
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
                    if(it.size > 1) {
                        if (it[0].equals(tpaylib, true)) {
                            mapVersion[it[0]] =
                                it[1].replace("\'".toRegex(), "").replace("\\+".toRegex(), "")
                                    .replace(
                                        "telepassLibCode".toRegex(), mapVersion["telepassLibCode"]
                                            ?: ""
                                    )
                        } else mapVersion[it[0]] = it[1].replace("\'".toRegex(), "")
                    }
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
    // Read module from name Version on Gradle file
    val mapModuleName: HashMap<String, String> = getVersionKeyFromModule()
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