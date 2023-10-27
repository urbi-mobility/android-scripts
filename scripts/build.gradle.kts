import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.*

tasks.register("uploadlib") {
    doLast {
        val appId = System.getProperty("args")
        val skipService = project.hasProperty("args") && (project.properties["args"] == "skipService")
        if (!skipService) {
            if (appId == "tpay")
                forcePullServiceFileAndCopyTPayLib()
            else
                forcePullServiceFileAndCopy()
        } else
            println("No service file is force pull and uploaded, you know what you are doing.......")

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
            writeChangelog(mapVersion, mapChangelog, map.key, "${map.key}/changelog.md", appId)
        }

        println("UPLOAD TASK COMPLETED.......")

    }
}

tasks.register("print-changelog-changed") {
    doLast {
        var mapChangelog = getChangelogMap()

        // Create map from versions in depend.gradle file
        val mapVersion: HashMap<String, String> = createMapVersion()

        println("Start to scan Changelog files.......")

////         Read Changelog Utility on Gradle file
        mapChangelog.forEach { map ->
            printChangelogChanged(mapVersion, mapChangelog, map.key, "${map.key}/changelog.md")
        }

        println("TASK COMPLETED.......")

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
                if (newLine.startsWith("modified:") || newLine.startsWith("modificato:")) {
                    val italianClearLine = newLine.replace("modificato:".toRegex(), "")
                    val clearLine = italianClearLine.replace("modified:".toRegex(), "")
                    val arrayRow = clearLine.split("/")
                    if (arrayRow.isNotEmpty() && mapVersion.containsKey(mapModuleName[arrayRow[0]])) {
                        val key = arrayRow[0]
                        val tagVersionKey = mapVersion[mapModuleName[key]]
                        println("Analyze ${key} version ${tagVersionKey}..............")
                        ByteArrayOutputStream().use { os ->
                            exec {
                                commandLine("./gradlew", "$key:formatKotlin")
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
    "composenavigation" to "COMPOSENAVIGATION_",
    "composeds" to "COMPOSEDS_",
    "common-state" to "COMMON-STATE_",
    "login" to "LOGIN_",
    "commonview" to "COMMONVIEW_",
    "urbiscan" to "SCN_",
    "urbisearch" to "SRC_",
    "urbipay" to "PAY_",
    "ticketlib" to "TCK_",
    "urbitaxi" to "TXI_",
    "evcharging" to "EVC_",
    "transpo" to "TRN_",
    "tripo" to "TRP_",
    "mobilitylib" to "MBL_",
    "shop" to "SHOP_",
    "profile" to "PROFILE_",
    "map" to "MAP_",
    "history" to "HISTORY_"
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
    "common-state" to  "commonStateVersion",
    "commonview" to "commonViewVersion",
    "composenavigation" to "composeNavigationVersion",
    "composeds" to "composeDsVersion",
    "history" to "historyVersion",
    "map" to "mapVersion",
    "profile" to "profileVersion",
    "shop" to "shopVersion",
    "login"  to "loginVersion",
    "telepasspaymodel" to "telepassModelVersion",
    "telepasspaynetwork" to "telepassNetworkVersion",
    "tpaylib" to "telepassLibVersion",
)

/**
 * if Module have third lib is inside this map
 */
fun haveModuleThird(key: String): Boolean {
    val list = arrayListOf(
        "utilitylib",
        "urbimodel",
        "urbicore" ,
        "designsystem",
        "urbipay",
        "ticketlib",
        "tripo",
        "common-state",
        "composenavigation",
        "composeds",
    )
    return list.contains(key)
}

fun createMapVersion(): HashMap<String, String> {
    val tpaylib = "telepassLibVersion"
    // Read Version on Gradle file
    val mapVersion: HashMap<String, String> = hashMapOf()
    val gradle = if(File("android-scripts/gradle/depend.gradle").exists()) File("android-scripts/gradle/depend.gradle") else File("$rootDir/android-urbi-framework/android-scripts/gradle/depend.gradle")
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
    pathFile: String,
    appId: String? = null
) {
    println("Reading Changelog $pathFile........")
    val format = SimpleDateFormat("yyyy-MM-dd")
    val dataNow = format.format(Date())
    val changelog = if(File(pathFile).exists()) File(pathFile) else File("$rootDir/android-urbi-framework/$pathFile")
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
    val avoidPublishTpay = project.properties["avoidPublishTpay"].toString()
    if (haveToWriteFile) {
        println("Update lib $key..........")
        if(appId != "tpay") {
            ByteArrayOutputStream().use { os ->
                val result = exec {
                    commandLine(
                        "./gradlew",
                        "$key:clean",
                        "$key:publishReleasePublicationToGitHubPackagesRepository",
                        if(avoidPublishTpay.toBoolean()) "" else" "$key:publishReleasePublicationToGitHubPackages2Repository"
                    )
                    standardOutput = os
                }
                println(os.toString())
            }
        } else {
            ByteArrayOutputStream().use { os ->
                val result = exec {
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
        if(haveModuleThird(key))
            ByteArrayOutputStream().use { os ->
            val result = exec {
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

fun printChangelogChanged(
    mapVersion: HashMap<String, String>,
    mapChangelog: HashMap<String, String>,
    key: String,
    pathFile: String
) {
    val format = SimpleDateFormat("yyyy-MM-dd")
    val dataNow = format.format(Date())
    val changelog = if(File(pathFile).exists()) File(pathFile) else File("$rootDir/android-urbi-framework/$pathFile")
    var lastIsUnrelase = false
    var haveToWriteFile = false
    // Read module from name Version on Gradle file
    val mapModuleName: HashMap<String, String> = getVersionKeyFromModule()
    val tagVersionKey = mapVersion[mapModuleName[key]]
    changelog.readLines().forEach { line ->
        if (line.startsWith("## [Unreleased]", true)) {
            lastIsUnrelase = true
        } else if (lastIsUnrelase && line.startsWith("##")) {
            return
        } else if (lastIsUnrelase && line.startsWith("-")) {
            lastIsUnrelase = false
            haveToWriteFile = true
        }
    }
    if (haveToWriteFile) {
        println("Changelod changed  $key..........")
    }
}


fun forcePullServiceFileAndCopyTPayLib() {
    println("Pull  urbi-services-providers-file")
    ByteArrayOutputStream().use { os ->
        val result = exec {
            commandLine("git", "submodule", "update","--recursive","--remote")
            standardOutput = os
        }
        println(os.toString())
    }
    println("Force Copy service file from urbi-services-providers-file")
    ByteArrayOutputStream().use { os ->
        val result = exec {
            commandLine("./gradlew", "tpaylib:copyServicesProvider","-Dargs=force")
            standardOutput = os
        }
        println(os.toString())
    }
}

fun forcePullServiceFileAndCopy() {
    println("Pull  urbi-services-providers-file")
    ByteArrayOutputStream().use { os ->
        val result = exec {
            commandLine("git", "submodule", "update","--recursive","--remote")
            standardOutput = os
        }
        println(os.toString())
    }
    println("Force Copy service file from urbi-services-providers-file")
    ByteArrayOutputStream().use { os ->
        val result = exec {
            commandLine("./gradlew", "mobilitylib:copyServicesProvider","-Dargs=force")
            standardOutput = os
        }
        println(os.toString())
    }
}

/**
 * This scripts read depend.gradle file where libs version are and update that if there is some change in their changelog
 */
tasks.register("update-version-lib") {
    doLast {
        val mapVersionUrbi = getVersionKeyFromModule()
        val mapVersionUrbiInverse = mapVersionUrbi.inverseMap()
        val keyToChangeVersion: HashSet<String> = HashSet()
        println("Start to scan Changelog files.......")
////         Read Changelog Utility on Gradle file
        mapVersionUrbi.forEach mapFor@{  map ->
            var lastIsUnrelase = false
            val pathFile = "${map.key}/changelog.md"
            try {
                val changelog =
                    if (File(pathFile).exists()) File(pathFile) else File("$rootDir/android-urbi-framework/$pathFile")
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
            }catch (e: Exception){
                println("Error for file $pathFile ${e.message}")
            }
        }
        if(keyToChangeVersion.isNotEmpty()){
            val newGradleDeep = arrayListOf<String>()
            val gradle = if(File("android-scripts/gradle/depend.gradle").exists()) File("android-scripts/gradle/depend.gradle") else File("$rootDir/android-urbi-framework/android-scripts/gradle/depend.gradle")
            var readVersion = false
            println("Reading /gradle/depend.gradle file for version......")
            gradle.forEachLine { line ->
                if (line.equals("//---End---//", true)) {
                    readVersion = false
                    newGradleDeep.add(line)
                }
                else if (readVersion) {
                    line.replace("\\s".toRegex(), "").let { lineW ->
                        lineW.split("=").let { it ->
                            if(it.size > 1 && mapVersionUrbiInverse.containsKey(it[0]) && keyToChangeVersion.contains(mapVersionUrbiInverse[it[0]])) {
                                val versionArray = it[1].replace("\'".toRegex(), "").split(".")
                                var mirrorVersion = versionArray[2]
                                var extraMirrorVersion = ""
                                if(!mirrorVersion.all { it.isDigit() } ){
                                    val arrMirror= mirrorVersion.split("-")
                                    mirrorVersion = arrMirror[0]
                                    extraMirrorVersion = arrMirror[1]
                                }
                                mirrorVersion = "${mirrorVersion.toInt()+1}"
                                var newVersionApp = "${versionArray[0]}.${versionArray[1]}.${mirrorVersion}"
                                if(extraMirrorVersion.isNotEmpty())
                                    newVersionApp = "$newVersionApp-$extraMirrorVersion"
                                newGradleDeep.add(line.replace(it[1].replace("\'".toRegex(), ""),newVersionApp))
                            }
                            else
                                newGradleDeep.add(line)
                        }
                    }
                }
                else if (line.equals("//---Module Version---//", true)) {
                    readVersion = true
                    newGradleDeep.add(line)
                }
                else
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
                val result = exec {
                    commandLine("./gradlew", "uploadlib", "-Pargs=skipService")
                    standardOutput = os
                }
                println(os.toString())
                println("Upload Libs RESULT$result")
            }
        }
        else
            println("No Version have updated")
    }
}

fun <K, V> Map<K, V>.inverseMap() = map { Pair(it.value, it.key) }.toMap()