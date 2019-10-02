import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.3.41"
}

group = "be.gvdenbro"
version = gitDescribeVersion()

repositories {
    jcenter()
}

dependencies {
    implementation(gradleApi())
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.beust:klaxon:5.0.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

fun gitDescribeVersion(): String {

    val stdOut = ByteArrayOutputStream()

    exec {
        commandLine("git", "describe", "--tags", "--long", "--always", "--match", "[0-9].[0-9]*")
        standardOutput = stdOut
        workingDir = rootDir
    }

    val gitDescribeMatchRegex = """(\d+)\.(\d+)-(\d+)-.*""".toRegex()
    val describe = stdOut.toString().trim()

    return gitDescribeMatchRegex.matchEntire(describe)
            ?.destructured
            ?.let { (major, minor, patch) ->
                "$major.$minor.$patch"
            }
            ?: throw GradleException("Cannot parse git describe '$describe'")
}