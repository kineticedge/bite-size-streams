
val oshi_version: String by project
val mapstruct_version: String by project

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))

    implementation("com.github.oshi:oshi-core:${oshi_version}")

    implementation("org.mapstruct:mapstruct:${mapstruct_version}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${mapstruct_version}")
}

application {
    mainClass.set("io.kineticedge.ks101.${project.name}.Main")
}