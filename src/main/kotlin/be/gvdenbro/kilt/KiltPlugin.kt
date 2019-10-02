package be.gvdenbro.kilt

import org.gradle.api.Plugin
import org.gradle.api.Project

open class KiltPluginExtension {
    var git: KiltPluginExtensionGit = KiltPluginExtensionGit()
}

open class KiltPluginExtensionGit {
    var userName: String? = null
    var userEmail: String? = null
}

class KiltPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}