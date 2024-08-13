plugins {
    id("java")
    id("application")
}

group = "net.puffish.textbot"
version = "1.0"

application {
    mainClass = "net.puffish.textbot.Main"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.2")
    implementation("de.siegmar:fastcsv:3.2.0")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.slf4j:slf4j-simple:2.0.16")
}