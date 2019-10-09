package be.gvdenbro.kilt

import com.beust.klaxon.Klaxon
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.net.HttpURLConnection
import java.net.URL

// https://api.slack.com/changelog/2018-04-truncating-really-long-messages
val slackMaxTextLength = 40_000

data class SlackMessage(val attachments: List<SlackMessageAttachment>)
data class SlackMessageAttachment(val title: String, val title_link: URL, val pretext: String?, val text: String, val color: String)

fun truncateIfNecessary(text: String): String {
    if (text.length > slackMaxTextLength) {
        return text.take(slackMaxTextLength - 1) + Typography.ellipsis
    }
    return text
}

open class SlackPostToChannelTask : DefaultTask() {

    lateinit var slackHookURL: URL
    lateinit var title: String
    lateinit var titleLink: URL
    var pretext: String? = null
    lateinit var text: String
    lateinit var color: String
    var shouldSendMessage: Boolean = true

    @TaskAction
    fun post() {

        logger.info("Will try to send slack message: $shouldSendMessage")

        if (shouldSendMessage) {

            logger.info("Posting to slack")

            val connection = slackHookURL.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000

            val slackMessage = SlackMessage(attachments = listOf(SlackMessageAttachment(title = title, title_link = titleLink, pretext = pretext, text = truncateIfNecessary(text), color = color)))

            connection.outputStream.use {
                it.write(klaxon.toJsonString(slackMessage).toByteArray())
            }

            logger.info("Posted to slack message ${slackMessage} with response code ${connection.responseCode}")
        }
    }

    companion object {
        val klaxon: Klaxon = Klaxon()
    }
}