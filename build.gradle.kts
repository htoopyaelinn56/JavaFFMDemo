plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.example.Main")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
    }
}

// Ensure the JAR is executable by including the Main-Class manifest entry
tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClass.get()
            )
        )
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Task to produce a release-ready JAR copied into build/release
val releaseDir = layout.buildDirectory.dir("release")

tasks.register<Copy>("releaseJar") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    val jarBaseName = "${project.name}-${project.version}.jar"
    into(releaseDir)
    rename { _ -> jarBaseName }
}
