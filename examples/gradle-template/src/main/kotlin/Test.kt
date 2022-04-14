import com.systema.eia.iot.tb.clients.ExtRestClient

fun main() {
    val erc = ExtRestClient("http://localhost:9090", "me", "secret")
}