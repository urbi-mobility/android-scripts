package urbi.co

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

abstract class RemoveGitHubPackage : DefaultTask() {

    companion object{
        const val ORG = "urbi-mobility"
    }
    @TaskAction
    fun removeUrbiPackages() {
        getDetailVersionPackage(ORG,"co.urbi.android.model")
//        val client = HttpClient.newHttpClient()
//        val request = HttpRequest.newBuilder()
//            .uri(URI("https://api.github.com/orgs/$ORG/packages?package_type=maven"))
//            .GET()
//            .header("Authorization", "Bearer ${System.getenv("GHP_API_KEY")}")
//            .header("X-GitHub-Api-Version", "2022-11-28")
//            .build()
//        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
//        val jsonParser = JsonParser()
//        try {
//            val tradeElement: JsonElement = jsonParser.parse(response.body())
//            tradeElement.asJsonArray.forEach{ element->
//                val packageName = element.asJsonObject.get("name").asString
//                getDetailVersionPackage(ORG,packageName)
//         }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    private fun getDetailVersionPackage(org: String, packageName: String){
        println("GET DETAIL FOR $packageName ORG $org")
        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.github.com/orgs/$org/packages/maven/$packageName/versions?per_page=100"))
            .GET()
            .header("Authorization", "Bearer ${System.getenv("GHP_API_KEY")}")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        val client = HttpClient.newHttpClient()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        val jsonParser = JsonParser()
        try {
            val tradeElement: JsonElement = jsonParser.parse(response.body())
            tradeElement.asJsonArray.forEach {element->
                val dataString = element.asJsonObject.get("updated_at").asString
                if(diffMonth(dataString)){
                    println("REMOVE $packageName ${element.asJsonObject.get("id").asString}")
                    removeUrbiPackage(ORG, packageName, element.asJsonObject.get("id").asString)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeUrbiPackage(org: String, packageName: String, packageVersion: String) {
        val request = HttpRequest.newBuilder()
            .uri(
                URI(
                    "https://api.github.com/orgs/${org}/packages/maven/${packageName}/versions/${packageVersion}"
                )
            )
            .DELETE()
            .header("Authorization", "Bearer ${System.getenv("GHP_API_KEY")}")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()
        val client = HttpClient.newHttpClient()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        println("RESPONSE ${response.statusCode()}")
    }

    private fun diffMonth(updateLib: String): Boolean{
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX")
        val data = LocalDate.parse(updateLib, formatter)
        val today = LocalDate.now()
        val period = Period.between(data,today)
        if(period.years >= 1)
            return true
        return Period.between(data,today).months > 8
    }
}
