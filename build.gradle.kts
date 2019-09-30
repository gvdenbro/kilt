plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.41"
}

group = "be.gvdenbro"
version = "0.0.6"

repositories {
    jcenter()
}

dependencies {
    implementation(gradleApi())
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
