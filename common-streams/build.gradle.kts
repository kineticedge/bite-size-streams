val kafka_version: String by project
val oshi_version: String by project
val junit_pioneer_version: String by project
val junit_version: String by project
val java_websocket_version: String by project

dependencies {

    implementation(project(":common"))
    implementation(project(":domain"))
    implementation(project(":producer"))

    implementation("org.apache.kafka:kafka-streams:${kafka_version}")

    implementation("org.java-websocket:Java-WebSocket:$java_websocket_version")
    implementation("com.github.oshi:oshi-core:${oshi_version}")

}

tasks.named<Test>("test") {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED"
    )
}