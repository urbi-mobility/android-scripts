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
