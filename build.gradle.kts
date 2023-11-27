plugins {
    idea
}

idea {
    module.isDownloadJavadoc = true
    module.isDownloadSources = true
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
tasks.register<urbi.co.RemoveGitHubPackage>("remove-package-urbi")
tasks.register<urbi.co.UploadGitHubPackage>("uploadlib")
tasks.register<urbi.co.UploadGitHubPackage>("uploadlibskip"){
    skipService=true
}
tasks.register<urbi.co.CommitVersionsTask>("commitversions")
tasks.register<urbi.co.UpdateVersionLibTask>("update-version-lib")


