package be.gvdenbro.kilt

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.net.HttpURLConnection
import java.net.URL

/**
 *  curl -X POST -H 'Content-type: application/json' --data "{'text': 'test', 'attachments': [{'fallback': 'blah','title': 'test','title_link': 'test','ts': 0}]}" https://hooks.slack.com/services/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
 */
open class SlackPostToChannelTask : DefaultTask() {

    lateinit var slackHookURL: URL
    lateinit var text: String
    lateinit var title: String
    lateinit var titleLink: URL

    @TaskAction
    fun post() {

        println("Posting to slack")

        val connection = slackHookURL.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-type", "application/json")
        connection.doOutput = true

        val body = body(text, title, titleLink).toByteArray()

        connection.outputStream.use {
            it.write(body)
        }

        println("Posted to slack with response code ${connection.responseCode}")
    }

    fun body(text: String, title: String, titleLink: URL): String {
        return """{
                    "text": "$text",
                    "attachments": [
                        {
                            "fallback": "blah",
                            "title": "$title",
                            "title_link": "$titleLink"
                        }
                    ]
                }"""
    }
}