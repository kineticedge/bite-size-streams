
val kafka_version: String by project

plugins {
    application
    distribution
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":common"))
    implementation(project(":producer"))
    implementation(project(":common-streams"))
    implementation("org.apache.kafka:kafka-streams:$kafka_version")
}

application {
    mainClass.set("io.kineticedge.ksd.${project.name}.Main")
    // applicationDefaultJvmArgs = listOf("-javaagent:./jmx_prometheus/jmx_prometheus_javaagent.jar=7071:./docker/streams-config.yml")
}


configurations.all {
    resolutionStrategy {
        force("io.prometheus:prometheus-metrics-model:1.4.0-SNAPSHOT")
    }
}

