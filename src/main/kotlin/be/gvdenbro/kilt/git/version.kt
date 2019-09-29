package be.gvdenbro.kilt.git

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream

open class GitDescribeTask : DefaultTask() {

    @TaskAction
    fun print() {
        println(gitDescribe())
    }

    fun gitDescribe(): String {

        val gitStdOut = ByteArrayOutputStream()

        project.exec {
            it.commandLine("git", "describe", "--tags", "--long", "--always", "--match", "[0-9].[0-9]*")
            it.standardOutput = gitStdOut
            it.workingDir = project.rootDir
        }

        return gitStdOut.toString().trim()
    }
}