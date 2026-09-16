
val oshi_version: String by project
val mapstruct_version: String by project

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))
    implementation("com.github.oshi:oshi-core:${oshi_version}")
}

application {
    mainClass.set("io.kineticedge.ks101.${project.name}.Main")
}