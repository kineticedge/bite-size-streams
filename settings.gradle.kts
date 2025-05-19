rootProject.name = "kafka-streams-tutorial"

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
