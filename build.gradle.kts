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
apply(from = "gradle/task.gradle")


