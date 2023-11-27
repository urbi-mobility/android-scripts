package urbi.co

import java.io.File

object Util {

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

    fun createMapVersion(rootDir: String): HashMap<String, String> {
        val tpaylib = "telepassLibVersion"
        // Read Version on Gradle file
        val mapVersion: HashMap<String, String> = hashMapOf()
        val gradle = when{
            (File("android-scripts/gradle/depend.gradle").exists())-> File("android-scripts/gradle/depend.gradle")
            (File("$rootDir/gradle/depend.gradle").exists())-> File("$rootDir/gradle/depend.gradle")
            else -> File("$rootDir/android-urbi-framework/android-scripts/gradle/depend.gradle")
        }
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

    fun <K, V> Map<K, V>.inverseMap() = map { Pair(it.value, it.key) }.toMap()

}
