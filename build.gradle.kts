import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

val kotlinVersion = "1.4.21"

plugins {
    java
    kotlin("jvm") version "1.4.21"
    `maven-publish`
    signing
    jacoco
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("org.jetbrains.dokka") version "0.10.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
    id("org.jlleitschuh.gradle.ktlint-idea") version "9.4.1"
    id("com.palantir.git-version") version "0.12.3"
}

group = "dev.reactant"
val gitVersion: groovy.lang.Closure<String> by extra
val versionDetails: groovy.lang.Closure<String> by extra
val details = versionDetails() as com.palantir.gradle.gitversion.VersionDetails
version = details.lastTag + if (!details.isCleanTag && !details.lastTag.endsWith("-SNAPSHOT")) "-SNAPSHOT" else ""
val isRelease = details.isCleanTag && !details.lastTag.endsWith("-SNAPSHOT")

println("Preparing build ${project.name} $version")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories {
    jcenter()
    mavenCentral()
    maven { url = URI.create("https://hub.spigotmc.org/nexus/content/repositories/snapshots") }
    maven { url = URI.create("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = URI.create("https://oss.sonatype.org/content/repositories/releases/") }
    maven { url = URI.create("https://repo.codemc.org/repository/maven-public") }
    maven { url = URI.create("https://jitpack.io") }
}

dependencies {

    implementation("org.bstats:bstats-bukkit:1.8") { isTransitive = false }

    api("io.reactivex.rxjava3:rxjava:3.0.9")
    api("io.reactivex.rxjava3:rxkotlin:3.0.1")

    api("com.gitlab.reactant:stretch-kotlin-jvm-bindings:16d52f2b3f")

    compileOnly("org.spigotmc:spigot-api:1.16.4-R0.1-SNAPSHOT")
    implementation(kotlin("reflect", kotlinVersion))

    testImplementation("org.spigotmc:spigot-api:1.16.4-R0.1-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.assertj:assertj-core:3.6.2")
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    disabledRules.set(setOf("no-wildcard-imports", "indent", "parameter-list-wrapping"))
    enableExperimentalRules.set(true)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.isEnabled = true
        csv.isEnabled = true
    }
}

val dokka = (tasks["dokka"] as DokkaTask).apply {
    outputFormat = "html"
}

val dokkaJavadoc by tasks.registering(DokkaTask::class) {
    outputFormat = "javadoc"
    outputDirectory = "$buildDir/javadoc"
}

val dokkaJar by tasks.registering(Jar::class) {
    dependsOn(dokka)
    archiveClassifier.set("dokka")
    from(tasks.dokka)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val shadowJar = (tasks["shadowJar"] as ShadowJar).apply {
    relocate("org.bstats", "dev.reactant.resquare")
}

val deployPlugin by tasks.registering(Copy::class) {
    dependsOn(shadowJar)
    System.getenv("PLUGIN_DEPLOY_PATH")?.let {
        from(shadowJar)
        into(it)
    }
}

val build = (tasks["build"] as Task).apply {
    arrayOf(
        sourcesJar,
        shadowJar,
        deployPlugin
    ).forEach { dependsOn(it) }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(shadowJar)
            artifact(javadocJar.get())
            artifact(dokkaJar.get())

            groupId = group.toString()
            artifactId = project.name
            version = version

            pom {
                name.set(project.name)
                description.set("Reactant Resquare UI Library")
                url.set("https://reactant.dev")
                licenses {
                    license {
                        name.set("GPL-3.0")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                    }
                }
                scm {
                    connection.set("scm:git:git@gitlab.com:reactant/resquare.git")
                    url.set("https://gitlab.com/reactant/resquare/")
                }

                developers {
                    developer {
                        id.set("setako")
                        name.set("Setako")
                        organization.set("Reactant Dev Team")
                        organizationUrl.set("https://gitlab.com/reactant")
                    }
                }
            }
        }
    }

    repositories {
        maven {

            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = System.getenv("SONATYPE_USERNAME")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

if (isRelease) {
    signing {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey?.replace("\\n", "\n"), signingPassword)
        sign(publishing.publications["maven"])
    }
}
