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
        val appId = System.getProperty("args")
        var mapChangelog: LinkedHashMap<String, String>;
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
    "urbiscan" to "SCN_",
    "commonview" to "COMMONVIEW_",
    "login" to "LOGIN_",
    "urbisearch" to "SRC_",
    "urbipay" to "PAY_",
    "ticketlib" to "TCK_",
    "urbitaxi" to "TXI_",
    "evcharging" to "EVC_",
    "transpo" to "TRN_",
    "tripo" to "TRP_",
    "mobilitylib" to "MBL_",
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
    "login"  to "loginVersion",
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
        "urbiscan",
        "commonview",
        "common-state",
        "composenavigation",
        "composeds",
        "login",
    )
    return list.contains(key)
}

/**
 * if Module is used inside Tpay
 */
fun haveModuleTPay(key: String): Boolean {
    val list = arrayListOf(
        "utilitylib",
        "urbimodel",
        "urbicore" ,
        "designsystem",
        "urbipay",
        "ticketlib",
        "tripo",
        "commonview",
        "common-state",
        "composenavigation",
        "composeds",
        "login",
        "mobilitylib",
        "ticketlib",
        "urbitaxi",
        "transpo",
        "urbiscan",
        "evcharging",
        "urbisearch"
    )
    return list.contains(key)
}

fun getBomVersionFromToml(): String {
    val tomlFile = if (File("android-scripts/gradle/libs-urbi.versions.toml").exists())
        File("android-scripts/gradle/libs-urbi.versions.toml")
    else
        File("$rootDir/android-urbi-framework/android-scripts/gradle/libs-urbi.versions.toml")
    return try {
        tomlFile.readLines()
            .firstOrNull { it.trimStart().startsWith("urbi-bom") }
            ?.split("=")?.getOrNull(1)
            ?.trim()?.removeSurrounding("\"") ?: "unknown"
    } catch (e: Exception) {
        println("Warning: cannot read BOM version from TOML: ${e.message}")
        "unknown"
    }
}

/**
 * Reads [Unreleased] entries from each changed module's changelog and writes a new entry
 * to bom/CHANGELOG.md. Returns the generated entry text (used for GitHub Release notes).
 */
fun writeBomChangelog(
    changedModules: Set<String>,
    moduleToVersionKey: Map<String, String>,
    currentVersions: Map<String, String>,
    dataNow: String,
    bomVersion: String
): String {
    val sb = StringBuilder()
    sb.appendLine("## [$bomVersion] $dataNow")
    sb.appendLine()

    changedModules.sorted().forEach { module ->
        val versionKey = moduleToVersionKey[module] ?: return@forEach
        val moduleVersion = currentVersions[versionKey] ?: "unknown"
        val changelogFile = when {
            File("$module/changelog.md").exists() -> File("$module/changelog.md")
            File("$rootDir/android-urbi-framework/$module/changelog.md").exists() ->
                File("$rootDir/android-urbi-framework/$module/changelog.md")
            else -> null
        }

        val entries = mutableListOf<String>()
        if (changelogFile != null) {
            var inUnreleased = false
            run breaking@{
                changelogFile.readLines().forEach { line ->
                    when {
                        line.startsWith("## [Unreleased]", ignoreCase = true) -> inUnreleased = true
                        inUnreleased && line.startsWith("##") -> return@breaking
                        inUnreleased && line.startsWith("-") -> entries.add(line)
                    }
                }
            }
        }

        if (entries.isNotEmpty()) {
            sb.appendLine("### $module — $moduleVersion")
            entries.forEach { sb.appendLine(it) }
            sb.appendLine()
        }
    }

    val newEntry = sb.toString().trimEnd()
    val bomChangelogFile = if (File("bom").exists())
        File("bom/CHANGELOG.md")
    else
        File("$rootDir/android-urbi-framework/bom/CHANGELOG.md")

    val existing = if (bomChangelogFile.exists()) "\n\n${bomChangelogFile.readText()}" else ""
    bomChangelogFile.writeText("$newEntry$existing")
    println("BOM changelog updated: ${bomChangelogFile.path}")
    return newEntry
}

/**
 * Finds the versioned changelog section matching [versionTag] (e.g. "UTL_6.26.6")
 * and returns its bullet-point entries.
 */
fun readModuleChangesForVersion(module: String, versionTag: String): List<String> {
    val pathFile = "$module/changelog.md"
    val changelogFile = when {
        File(pathFile).exists() -> File(pathFile)
        File("$rootDir/android-urbi-framework/$pathFile").exists() ->
            File("$rootDir/android-urbi-framework/$pathFile")
        else -> return emptyList()
    }
    val entries = mutableListOf<String>()
    var inSection = false
    run breaking@{
        changelogFile.readLines().forEach { line ->
            when {
                !inSection && line.startsWith("## [") && line.contains(versionTag) -> inSection = true
                inSection && line.startsWith("##") -> return@breaking
                inSection && line.startsWith("-") -> entries.add(line)
            }
        }
    }
    return entries
}

/**
 * Creates a GitHub Release for the given BOM version using the gh CLI.
 * Skip with -PskipRelease=true.
 */
fun createGithubRelease(bomVersion: String, releaseNotes: String) {
    val tempFile = File.createTempFile("bom-release-notes", ".md")
    try {
        tempFile.writeText(releaseNotes)
        ByteArrayOutputStream().use { os ->
            exec {
                commandLine(
                    "gh", "release", "create", "bom-$bomVersion",
                    "--repo", "urbi-mobility/android-urbi-framework",
                    "--title", "BoM $bomVersion",
                    "--notes-file", tempFile.absolutePath
                )
                standardOutput = os
            }
            println(os.toString())
        }
        println("GitHub Release created: bom-$bomVersion")
    } catch (e: Exception) {
        println("Warning: GitHub Release creation failed: ${e.message}")
    } finally {
        tempFile.delete()
    }
}

fun createMapVersion(): HashMap<String, String> {
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
                        mapVersion[it[0]] = it[1].replace("\'".toRegex(), "")
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
    val publishThirdParty = project.properties["publishThirdParty"].toString()
    if (haveToWriteFile) {
        println("Update lib $key..........")
        if(avoidPublishTpay.toBoolean()){
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
        else if(haveModuleTPay(key)){
            ByteArrayOutputStream().use { os ->
                val result = exec {
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
        if(haveModuleThird(key) && (publishThirdParty.toBoolean()?: false))
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

fun updatePatchVersion(versionToUpgrade: List<String>): String {
    val versionArray = versionToUpgrade[1].replace("\'".toRegex(), "").split(".")
    var patchVersion = versionArray[2]
    var extraPatchVersion = ""
    if(!patchVersion.all { it.isDigit() } ){
        val arrMirror= patchVersion.split("-")
        patchVersion = arrMirror[0]
        extraPatchVersion = arrMirror[1]
    }
    patchVersion = "${patchVersion.toInt()+1}"
    var newVersionApp = "${versionArray[0]}.${versionArray[1]}.${patchVersion}"
    if(extraPatchVersion.isNotEmpty())
        newVersionApp = "$newVersionApp-$extraPatchVersion"
    return newVersionApp
}

fun updateMinorVersion(versionToUpgrade: List<String>): String {
    val versionArray = versionToUpgrade[1].replace("\'".toRegex(), "").split(".")
    var minorVersion = versionArray[1]
    minorVersion = "${minorVersion.toInt()+1}"
    var newVersionApp = "${versionArray[0]}.${minorVersion}.0"
    return newVersionApp
}

fun updateMajorVersion(versionToUpgrade: List<String>): String {
    val versionArray = versionToUpgrade[1].replace("\'".toRegex(), "").split(".")
    var majorVersion = versionArray[0]
    majorVersion = "${majorVersion.toInt()+1}"
    var newVersionApp = "${majorVersion}.0.0"
    return newVersionApp
}

/**
 * This script reads the `depend.gradle` file, where library versions are defined, and upgrades
 * the desired version if there are changes in its changelog.
 *
 * The level of updating can be: patch, minor and major, depending of what has been changed
 * - **patch**: bugfixing
 * - **minor**: small feature with no breaking changes
 * - **major**: features with breaking changes
 *
 * Pass the level as an argument, e.g., `-Dargs=minor`. If no argument is passed, `patch` is the default level.
 *
 * Then, upload a new BoM with the new versions (you have to manually
 * upgrade BoM version) and the upload libs.
 */
tasks.register("upgrade-lib-version") {
    doLast {
        val publishThirdParty = project.properties["publishThirdParty"].toString()

        val levelUpdate = System.getProperty("args")?: "patch"
        println("Level update: $levelUpdate")
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
                                val newVersionApp = when(levelUpdate){
                                    "minor" -> updateMinorVersion(it)
                                    "major" -> updateMajorVersion(it)
                                    else -> updatePatchVersion(it)
                                }
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

            val format = SimpleDateFormat("yyyy-MM-dd")
            val dataNow = format.format(Date())
            val bomVersion = getBomVersionFromToml()
            val currentVersions = createMapVersion()
            val releaseNotes = writeBomChangelog(keyToChangeVersion, mapVersionUrbi, currentVersions, dataNow, bomVersion)
            val skipRelease = project.properties["skipRelease"]?.toString()?.toBoolean() ?: false
            if (!skipRelease) createGithubRelease(bomVersion, releaseNotes)

            println("Upload BoM")
            ByteArrayOutputStream().use { os ->
                val result = exec {
                    if(publishThirdParty.toBoolean()?: false) {
                        commandLine("./gradlew", "bom:publishAllBom")
                    } else {
                        commandLine("./gradlew", "bom:publishAllBomNoThirdParty")
                    }
                    standardOutput = os
                }
                println(os.toString())
                println("Upload BoM: $result")
            }
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

/**
 * Standalone BoM release — no lib upload required.
 *
 * For each module, reads the changelog section that matches the module's current version
 * in depend.gradle (e.g. ## [UTL_6.26.6]). Writes bom/CHANGELOG.md, publishes the BoM to
 * GitHub Packages, and creates a GitHub Release.
 *
 * Usage:
 *   ./gradlew release-bom                                  # all modules with versioned entries
 *   ./gradlew release-bom -Pmodules=utilitylib,composeds   # explicit subset
 *   ./gradlew release-bom -PskipRelease=true               # skip GitHub Release creation
 */
tasks.register("release-bom") {
    doLast {
        val format = SimpleDateFormat("yyyy-MM-dd")
        val dataNow = format.format(Date())
        val bomVersion = getBomVersionFromToml()
        val changelogPrefixes = getChangelogMap()       // module -> tag prefix, e.g. "UTL_"
        val versionKeys = getVersionKeyFromModule()     // module -> version key, e.g. "utilityVersion"
        val currentVersions = createMapVersion()        // version key -> version string

        val modulesParam = project.properties["modules"]?.toString()
        val candidateModules: Set<String> = if (!modulesParam.isNullOrBlank()) {
            modulesParam.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        } else {
            changelogPrefixes.keys.toSet()
        }

        val sb = StringBuilder()
        sb.appendLine("## [$bomVersion] $dataNow")
        sb.appendLine()

        var hasEntries = false
        candidateModules.sorted().forEach { module ->
            val prefix = changelogPrefixes[module] ?: return@forEach
            val versionKey = versionKeys[module] ?: return@forEach
            val version = currentVersions[versionKey] ?: return@forEach
            val versionTag = "$prefix$version"
            val entries = readModuleChangesForVersion(module, versionTag)
            if (entries.isNotEmpty()) {
                hasEntries = true
                sb.appendLine("### $module — $version")
                entries.forEach { sb.appendLine(it) }
                sb.appendLine()
            }
        }

        if (!hasEntries) {
            println("No versioned changelog entries found for current library versions. Nothing to release.")
            return@doLast
        }

        val releaseNotes = sb.toString().trimEnd()

        val bomChangelogFile = if (File("bom").exists())
            File("bom/CHANGELOG.md")
        else
            File("$rootDir/android-urbi-framework/bom/CHANGELOG.md")
        val existing = if (bomChangelogFile.exists()) "\n\n${bomChangelogFile.readText()}" else ""
        bomChangelogFile.writeText("$releaseNotes$existing")
        println("BoM changelog updated: ${bomChangelogFile.path}")

        val publishThirdParty = project.properties["publishThirdParty"]?.toString()?.toBoolean() ?: false
        println("Publishing BoM to GitHub Packages...")
        ByteArrayOutputStream().use { os ->
            val result = exec {
                if (publishThirdParty) {
                    commandLine("./gradlew", "bom:publishAllBom")
                } else {
                    commandLine("./gradlew", "bom:publishAllBomNoThirdParty")
                }
                standardOutput = os
            }
            println(os.toString())
            println("Publish BoM: $result")
        }

        val skipRelease = project.properties["skipRelease"]?.toString()?.toBoolean() ?: false
        if (!skipRelease) createGithubRelease(bomVersion, releaseNotes)
    }
}

/**
 * This scripts read depend.gradle file where libs version are and upgrade patch versionif there is some change in their changelog, and then upload new libs
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
            println("Upload libsUrbi............")
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
