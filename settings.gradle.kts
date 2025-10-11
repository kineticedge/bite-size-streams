rootProject.name = "bite-size-streams"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
    }
}

include("domain")
include("common")
include("common-streams")
include("producer")
include("tutorial")
include("testing-app")
