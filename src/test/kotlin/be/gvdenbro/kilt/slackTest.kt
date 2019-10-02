package be.gvdenbro.kilt

import com.beust.klaxon.Klaxon
import org.junit.Test
import java.net.URL

class KlaxonTests {

    @Test
    fun klaxonMessage() {

        val slackMessage = SlackMessage(attachments = listOf(SlackMessageAttachment(text = "text", pretext = "pretext", title = "title", title_link = URL("https://tool.gocd-server.d00sv179:6666/go/tab/build/detail/QA-ztest/10/merge/1/merge"), color = "good")))
        val toJsonString = Klaxon().toJsonString(slackMessage)

        println(toJsonString)
    }
}