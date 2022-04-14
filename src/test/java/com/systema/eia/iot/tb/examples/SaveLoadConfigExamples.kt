import com.systema.eia.iot.tb.clients.ExtRestClient
import org.thingsboard.server.common.data.page.PageLink

fun main() {
    val bundles = ExtRestClient("http://${System.getenv("TB_HOST")}:${System.getenv("TB_PORT")}")
        .getWidgetsBundles(PageLink(1)).data
    bundles.forEach { println(it.title) }

}