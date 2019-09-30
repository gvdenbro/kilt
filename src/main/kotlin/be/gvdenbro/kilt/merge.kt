package be.gvdenbro.kilt

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

open class GitMergeableTask : DefaultTask() {

    lateinit var fromBranch: String
    lateinit var toBranch: String

    @TaskAction
    fun mergeable() {
        throw GradleException("Merge from [${fromBranch}] to [${toBranch}] is not possible. Please do it manually.")
    }
}