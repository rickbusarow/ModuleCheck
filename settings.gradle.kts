pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    jcenter()
  }
}

plugins {
  id("com.gradle.enterprise").version("3.4.1")
}

val VERSION: String by extra.properties

gradleEnterprise {
  buildScan {

    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    publishAlways()

    tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")
    tag(VERSION)

    val githubActionID = System.getenv("GITHUB_ACTION")

    if (!githubActionID.isNullOrBlank()) {
      link(
          "WorkflowURL",
          "https://github.com/" +
              System.getenv("GITHUB_REPOSITORY") +
              "/pull/" +
              System.getenv("PR_NUMBER") +
              "/checks?check_run_id=" +
              System.getenv("GITHUB_RUN_ID")
      )
    }
  }
}

rootProject.name = "ModuleCheck"

include (":plugin")
