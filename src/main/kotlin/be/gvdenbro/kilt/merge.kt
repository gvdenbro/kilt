package be.gvdenbro.kilt

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.ExecException
import java.io.ByteArrayOutputStream

/**
 * First checks if two branches are mergeable, then merges.
 * https://stackoverflow.com/questions/501407/is-there-a-git-merge-dry-run-option/6283843#6283843
 */
open class GitMergeTask : DefaultTask() {

    lateinit var source: String
    lateinit var destination: String
    lateinit var userName: String
    lateinit var userEmail: String

    @TaskAction
    fun merge() {

        assertIsMergeable()

        try {
            config("user.email", userEmail) // needed for merging/pushing
            config("user.name", userName)
            checkout(destination)
            merge(source)
            push(destination)
        } finally {
            checkout(source)
        }
    }

    fun assertIsMergeable() {

        logger.info("Checking if source ${source} can be merged into ${destination}")

        val currentRevision: String = revision(source)
        fetch(destination)
        val mergeBase: String = mergeBase("FETCH_HEAD", source)
        val mergeTree: String = mergeTree(mergeBase, source, "FETCH_HEAD")

        if (mergeTree.contains("+<<<<<<< .our")) {
            throw GradleException("Conflicts detected between source [${source}] and destination [${destination}]. Please merge manually. On a clean repo: git checkout $destination; git pull; git merge $currentRevision; And then fix conflicts and push.\nMerge tree: ${mergeTree}")
        }

        logger.info("Source ${source} can be merged into ${destination}")
    }

    private fun revision(source: String): String {
        // git rev-parse HEAD
        logger.info("Getting revision of ${source}")

        val revisionStdout = ByteArrayOutputStream()
        val revisionStderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine("git", "rev-parse", "HEAD")
            it.workingDir = project.rootDir
            it.standardOutput = revisionStdout
            it.errorOutput = revisionStderr
        }

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't get revsion of ${source}: $revisionStderr")
        }

        val revision = revisionStdout.toString().trim()

        logger.info("${source} is at revision ${revision}")

        return revision
    }

    private fun fetch(destination: String) {

        logger.info("Fetching ${destination}")

        val fetchStdout = ByteArrayOutputStream()
        val fetchStderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine("git", "fetch", "origin", destination)
            it.workingDir = project.rootDir
            it.standardOutput = fetchStdout
            it.errorOutput = fetchStderr
        }

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't fetch origin/$destination: $fetchStderr")
        }

        logger.info(fetchStdout.toString().trim())
        logger.info("${destination} fetched")
    }

    private fun mergeBase(commit1: String, commit2: String): String {

        logger.info("Looking for merge base between ${commit1} and ${commit2}")

        val mergeBaseStdout = ByteArrayOutputStream()
        val mergeBaseStderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine("git", "merge-base", commit1, commit2)
            it.workingDir = project.rootDir
            it.standardOutput = mergeBaseStdout
            it.errorOutput = mergeBaseStderr
        }

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't merge-base $commit1 $commit2: $mergeBaseStderr")
        }

        val mergeBaseOut = mergeBaseStdout.toString().trim()

        logger.info("Found merge base ${mergeBaseOut} between ${commit1} and ${commit2}")

        return mergeBaseOut
    }

    private fun mergeTree(baseTree: String, branch1: String, branch2: String): String {

        logger.info("Calculating merge tree from ${baseTree} between ${branch1} and ${branch2}")

        val mergeTreeStdout = ByteArrayOutputStream()
        val mergeTreeStderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine("git", "merge-tree", baseTree, branch1, branch2)
            it.workingDir = project.rootDir
            it.standardOutput = mergeTreeStdout
            it.errorOutput = mergeTreeStderr
        }

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't merge-tree $branch1 $branch2: $mergeTreeStderr")
        }

        return mergeTreeStdout.toString().trim()
    }

    private fun config(key: String, value: String) {

        logger.info("Configuring ${key} to $value")

        val configStdout = ByteArrayOutputStream()
        val configStderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine("git", "config", key, """"$value"""")
            it.workingDir = project.rootDir
            it.standardOutput = configStdout
            it.errorOutput = configStderr
        }

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't set config $key with value $value: $configStderr")
        }

        logger.info("Configured $key to $value")
    }

    private fun checkout(branch: String) {

        logger.info("Checking out ${branch}")

        val checkoutStdout = ByteArrayOutputStream()
        val checkoutStderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine("git", "checkout", branch)
            it.workingDir = project.rootDir
            it.standardOutput = checkoutStdout
            it.errorOutput = checkoutStderr
        }

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't checkout $branch: $checkoutStderr")
        }

        logger.info(checkoutStdout.toString().trim())
        logger.info("${branch} checked out")
    }

    private fun merge(branch: String) {

        logger.info("Merging ${branch}")

        val mergeStdout = ByteArrayOutputStream()
        val mergeStderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine("git", "merge", branch)
            it.workingDir = project.rootDir
            it.standardOutput = mergeStdout
            it.errorOutput = mergeStderr
        }

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't merge $branch: $mergeStderr")
        }

        logger.info(mergeStdout.toString().trim())
        logger.info("${branch} merged")
    }

    private fun push(branch: String) {

        logger.info("Pushing ${branch}")

        val pushStdout = ByteArrayOutputStream()
        val pushStderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine("git", "push", "origin", branch)
            it.workingDir = project.rootDir
            it.standardOutput = pushStdout
            it.errorOutput = pushStderr
        }

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't push $branch: $pushStderr")
        }

        logger.info(pushStdout.toString().trim())
        logger.info("${branch} pushed")
    }
}