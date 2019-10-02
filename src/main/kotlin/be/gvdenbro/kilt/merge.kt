package be.gvdenbro.kilt

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult
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

        logger.info("Getting revision of ${source}")

        val (exec, revision, stderr) = execute("git", "rev-parse", "--short", "HEAD")

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't get revsion of ${source}: $stderr")
        }

        logger.info("${source} is at revision ${revision}")

        return revision
    }

    private fun fetch(destination: String) {

        logger.info("Fetching ${destination}")

        val (exec, stdout, stderr) = execute("git", "fetch", "origin", destination)

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't fetch origin/$destination: $stderr")
        }

        logger.info(stdout)
        logger.info("${destination} fetched")
    }

    private fun mergeBase(commit1: String, commit2: String): String {

        logger.info("Looking for merge base between ${commit1} and ${commit2}")

        val (exec, mergeBaseOut, stderr) = execute("git", "merge-base", commit1, commit2)

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't merge-base $commit1 $commit2: $stderr")
        }

        logger.info("Found merge base ${mergeBaseOut} between ${commit1} and ${commit2}")

        return mergeBaseOut
    }

    private fun mergeTree(baseTree: String, branch1: String, branch2: String): String {

        logger.info("Calculating merge tree from ${baseTree} between ${branch1} and ${branch2}")

        val (exec, mergeTreeStdout, stderr) = execute("git", "merge-tree", baseTree, branch1, branch2)

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't merge-tree $branch1 $branch2: $stderr")
        }

        return mergeTreeStdout
    }

    private fun config(key: String, value: String) {

        logger.info("Configuring ${key} to $value")

        val (exec, _, stderr) = execute("git", "config", key, """"$value"""")

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't set config $key with value $value: $stderr")
        }

        logger.info("Configured $key to $value")
    }

    private fun checkout(branch: String) {

        logger.info("Checking out ${branch}")

        val (exec, stdout, stderr) = execute("git", "checkout", branch)

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't checkout $branch: $stderr")
        }

        logger.info(stdout)
        logger.info("${branch} checked out")
    }

    private fun merge(branch: String) {

        logger.info("Merging ${branch}")

        val (exec, stdout, stderr) = execute("git", "merge", branch)

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't merge $branch: $stderr")
        }

        logger.info(stdout)
        logger.info("${branch} merged")
    }

    private fun push(branch: String) {

        logger.info("Pushing ${branch}")

        val (exec, stdout, stderr) = execute("git", "push", "origin", branch)

        if (exec.exitValue != 0) {
            throw ExecException("Couldn't push $branch: $stderr")
        }

        logger.info(stdout)
        logger.info("${branch} pushed")
    }

    private fun execute(vararg args: String): ExecutionResult {

        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        val exec = project.exec {
            it.isIgnoreExitValue = true
            it.commandLine(*args)
            it.workingDir = project.rootDir
            it.standardOutput = stdout
            it.errorOutput = stderr
        }

        return ExecutionResult(exec, stdout.toString().trim(), stderr.toString().trim())
    }
}

data class ExecutionResult(val exec: ExecResult, val stdout: String, val stderr: String)