package be.gvdenbro.kilt

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

import java.net.URL

open class KiltConfig {

    val git = KiltGitConfig()
    val slack = KiltSlackConfig()
    val gocd = KiltGocdConfig()
    var merges = KiltMergeConfig()

    fun git(configure: KiltGitConfig.() -> Unit) {
        git.configure()
    }

    fun git(closure: Closure<*>): KiltGitConfig {
        return ConfigureUtil.configure(closure, git)
    }

    fun slack(configure: KiltSlackConfig.() -> Unit) {
        slack.configure()
    }

    fun slack(closure: Closure<*>): KiltSlackConfig {
        return ConfigureUtil.configure(closure, slack)
    }

    fun gocd(configure: KiltGocdConfig.() -> Unit) {
        gocd.configure()
    }

    fun gocd(closure: Closure<*>): KiltGocdConfig {
        return ConfigureUtil.configure(closure, gocd)
    }

    fun merge(configure: KiltMergeConfig.() -> Unit) {
        merges.configure()
    }

    fun merge(closure: Closure<*>): KiltMergeConfig {
        return ConfigureUtil.configure(closure, merges)
    }
}

open class KiltGitConfig {
    lateinit var userName: String
    lateinit var userEmail: String
}

open class KiltSlackConfig {
    lateinit var hookURL: String
}

open class KiltGocdConfig {
    lateinit var url: String
    lateinit var pipeline: String
    lateinit var pipelineCounter: String
    lateinit var stage: String
    lateinit var stageCounter: String
    lateinit var job: String
}

open class KiltMergeConfig {

    val mappings = ArrayList<KiltMergeMappingConfig>()

    fun map(configure: KiltMergeMappingConfig.() -> Unit) {
        val mergeMapping = KiltMergeMappingConfig()
        mergeMapping.configure()
        mappings.add(mergeMapping)
    }

    fun map(closure: Closure<*>): KiltMergeMappingConfig {
        val mergeMapping = KiltMergeMappingConfig()
        mappings.add(mergeMapping)
        return ConfigureUtil.configure(closure, mergeMapping)
    }
}

open class KiltMergeMappingConfig {

    lateinit var source: String
    lateinit var destination: String

    operator fun component1(): String {
        return source
    }

    operator fun component2(): String {
        return destination
    }
}

/**
 * this plugin integrates GoCD/Git/Slack into one whole. It is not meant to be reusable.
 */
class KiltPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.run {

            val config = extensions.create("kilt", KiltConfig::class.java)

            afterEvaluate {

                config.merges.mappings.forEach { (source, destination) ->

                    val mergeTask = tasks.create("merge${source.capitalize()}To${destination.capitalize()}", GitMergeTask::class.java) {
                        it.group = "gocd"
                        it.description = "Tries to merge current checkout of branch '${source}' into remote branch '${destination}'"
                        it.source = source
                        it.destination = destination
                        it.userName = config.git.userName
                        it.userEmail = config.git.userEmail

                        it.finalizedBy("slackMerge${source.capitalize()}To${destination.capitalize()}")
                    }

                    tasks.register("slackMerge${source.capitalize()}To${destination.capitalize()}", SlackPostToChannelTask::class.java) { slackTask ->
                        slackTask.doFirst {
                            slackTask.group = "gocd"
                            slackTask.description = "Tries to send message to slack with merge status"
                            configureSlackTask(slackTask, mergeTask, config)
                        }
                    }
                }
            }
        }
    }

    fun configureSlackTask(slackTask : SlackPostToChannelTask, mergeTask: GitMergeTask, config: KiltConfig) {

        val gocdConfig = config.gocd

        slackTask.slackHookURL = URL(config.slack.hookURL)
        slackTask.titleLink = URL("${gocdConfig.url}/tab/build/detail/${gocdConfig.pipeline}/${gocdConfig.pipelineCounter}/${gocdConfig.stage}/${gocdConfig.stageCounter}/${gocdConfig.job}")

        if (mergeTask.state.failure != null) {
            val cause = if (mergeTask.state.failure?.cause != null) mergeTask.state.failure?.cause?.message else mergeTask.state.failure?.message
            slackTask.title = "${gocdConfig.pipeline}/${gocdConfig.stage} failed merge attempt"
            slackTask.pretext = "Failed merging ${mergeTask.source} to ${mergeTask.destination}."
            slackTask.text = "Please fix it manually: ${cause}"
            slackTask.color = "danger"
        } else {
            slackTask.title = "${gocdConfig.pipeline}/${gocdConfig.stage} merge succeeded"
            slackTask.text = "Succeeded merging ${mergeTask.source} to ${mergeTask.destination}"
            slackTask.color = "good"
        }
    }
}