package be.gvdenbro.kilt

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

/**
 * Non-destructive way of checking if a merge will succeed.
 * https://stackoverflow.com/questions/501407/is-there-a-git-merge-dry-run-option/6283843#6283843
 */
open class GitMergeableTask : DefaultTask() {

    var source: String = "HEAD"
    lateinit var destination: String

    @TaskAction
    fun mergeable() {

        fetch(destination)
        val mergeBase: String = mergeBase("FETCH_HEAD", source)
        val mergeTree: String = mergeTree(mergeBase, source, "FETCH_HEAD")

        if (mergeTree.contains("+<<<<<<< .our")) {
            throw GradleException("Conflicts detected between source [${source}] and destination [${destination}]. Please merge manually. Merge tree:\n${mergeTree}")
        }
    }

    private fun fetch(destination: String) {

        val fetchStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "fetch", "origin", destination)
            it.workingDir = project.rootDir
            it.standardOutput = fetchStdout
        }
    }

    private fun mergeBase(commit1: String, commit2: String): String {

        val mergeBaseStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "merge-base", commit1, commit2)
            it.workingDir = project.rootDir
            it.standardOutput = mergeBaseStdout
        }

        return mergeBaseStdout.toString().trim()
    }

    private fun mergeTree(baseTree: String, branch1: String, branch2: String): String {

        val mergeTreeStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "merge-tree", baseTree, branch1, branch2)
            it.workingDir = project.rootDir
            it.standardOutput = mergeTreeStdout
        }

        return mergeTreeStdout.toString().trim()
    }
}