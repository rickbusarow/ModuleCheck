plugins {
    kotlin("jvm")
}

dependencies {
//    api(project(path = ":sample:library-a"))
    api(project(path = ":sample:nested:library-c"))
    api(project(path = ":sample:library-b"))
//  testImplementation(project(path = ":sample:nested:library-c"))
}
