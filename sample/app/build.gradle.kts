plugins {
  kotlin("jvm")
}

dependencies {

  // floating comment

  // comment before the dependency
  api(project(path = ":sample:library-a")) // in-line comment


  api(project(path = ":sample:nested:library-c"))
  api(project(path = ":sample:library-b"))
  testImplementation(project(path = ":sample:nested:library-c"))
}
