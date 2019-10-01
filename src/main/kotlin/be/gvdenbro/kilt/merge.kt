package be.gvdenbro.kilt

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

/**
 * First checks if two branches are mergeable, then merges.
 * https://stackoverflow.com/questions/501407/is-there-a-git-merge-dry-run-option/6283843#6283843
 */
open class GitMergeTask : DefaultTask() {

    lateinit var source: String
    lateinit var destination: String

    @TaskAction
    fun merge() {

        assertIsMergeable()

        checkout(destination)
        merge(source)
        push(destination)
    }

    fun assertIsMergeable() {

        println("Checking if source ${source} can be merged into ${destination}")

        fetch(destination)
        val mergeBase: String = mergeBase("FETCH_HEAD", source)
        val mergeTree: String = mergeTree(mergeBase, source, "FETCH_HEAD")

        if (mergeTree.contains("+<<<<<<< .our")) {
            throw GradleException("Conflicts detected between source [${source}] and destination [${destination}]. Please merge manually. Merge tree:\n${mergeTree}")
        }

        println("Source ${source} can be merged into ${destination}")
    }

    private fun fetch(destination: String) {

        println("Fetching ${destination}")

        val fetchStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "fetch", "origin", destination)
            it.workingDir = project.rootDir
            it.standardOutput = fetchStdout
        }

        println(fetchStdout.toString())
        println("${destination} fetched")
    }

    private fun mergeBase(commit1: String, commit2: String): String {

        println("Looking for merge base between ${commit1} and ${commit2}")

        val mergeBaseStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "merge-base", commit1, commit2)
            it.workingDir = project.rootDir
            it.standardOutput = mergeBaseStdout
        }

        val mergeBase = mergeBaseStdout.toString().trim()

        println("Found merge base ${mergeBase} between ${commit1} and ${commit2}")

        return mergeBase
    }

    private fun mergeTree(baseTree: String, branch1: String, branch2: String): String {

        println("Calculating merge tree from ${baseTree} between ${branch1} and ${branch2}")

        val mergeTreeStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "merge-tree", baseTree, branch1, branch2)
            it.workingDir = project.rootDir
            it.standardOutput = mergeTreeStdout
        }

        return mergeTreeStdout.toString().trim()
    }

    private fun checkout(branch: String) {

        println("Checking out ${branch}")

        val checkoutStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "checkout", branch)
            it.workingDir = project.rootDir
            it.standardOutput = checkoutStdout
        }

        println(checkoutStdout.toString())
        println("${branch} checked out")
    }

    private fun merge(branch: String) {

        println("Merging ${branch}")

        val mergeStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "merge", branch)
            it.workingDir = project.rootDir
            it.standardOutput = mergeStdout
        }

        println(mergeStdout.toString())
        println("${branch} merged")
    }

    private fun push(branch: String) {

        println("Pushing ${branch}")

        val pushStdout = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "push", "origin", branch)
            it.workingDir = project.rootDir
            it.standardOutput = pushStdout
        }

        println(pushStdout.toString())
        println("${branch} pushed")
    }
}