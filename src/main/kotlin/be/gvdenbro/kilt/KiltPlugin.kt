package be.gvdenbro.kilt

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.net.URL

open class KiltConfig {

    val git = KiltGitConfig()
    val slack = KiltSlackConfig()
    val gocd = KiltGocdConfig()
    val mergeDetails = LinkedHashMap<String, String>()

    fun git(configure: KiltGitConfig.() -> Unit) {
        git.configure()
    }

    fun slack(configure: KiltSlackConfig.() -> Unit) {
        slack.configure()
    }

    fun gocd(configure: KiltGocdConfig.() -> Unit) {
        gocd.configure()
    }
}

open class KiltGitConfig {
    lateinit var userName: String
    lateinit var userEmail: String
}

open class KiltSlackConfig {
    lateinit var hookURL: URL
}

open class KiltGocdConfig {
    lateinit var url: URL
}

/**
 * this plugin integrates GoCD/Git/Slack into one whole. It is not meant to be reusable.
 */
class KiltPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.run {

            val config = extensions.create("kilt", KiltConfig::class.java)

            config.mergeDetails.forEach { source, destination ->

                tasks.register("merge$source$destination", GitMergeTask::class.java) {
                    it.source = source
                    it.destination = destination
                    it.userName = config.git.userName
                    it.userEmail = config.git.userEmail
                }
            }
        }
    }
}