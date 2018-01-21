import com.bmuschko.gradle.docker.tasks.DockerInfo
import com.bmuschko.gradle.docker.tasks.DockerVersion
import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerLogsContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.container.extras.DockerWaitHealthyContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage
import java.io.IOException

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.bmuschko:gradle-docker-plugin:3.2.2")
    }
}


plugins {
    application
    kotlin("jvm") version "1.2.20"
    id("com.bmuschko.docker-remote-api") version "3.2.2"
}

application {
    mainClassName = "samples.HelloWorldKt"
}

repositories {
    jcenter()
}

dependencies {
    val kafkaVersion = "0.11.0.2"

    compile(kotlin("stdlib-jdk8"))

    compile("org.apache.kafka:kafka-streams:$kafkaVersion")
    compile("org.apache.kafka:kafka_2.12:0.11.0.2")
    compile("io.github.microutils:kotlin-logging:1.5.3")

    runtime("ch.qos.logback:logback-classic:1.2.3")
    runtime("org.slf4j:log4j-over-slf4j:1.7.25")

    testCompile("junit:junit:4.12")
    testCompile("org.assertj:assertj-core:3.9.0")
    testCompile("com.101tec:zkclient:0.10")
    testCompile("org.apache.kafka:kafka-clients:$kafkaVersion")
    testCompile("com.natpryce:hamkrest:1.4.2.2")

    testRuntime("org.jetbrains.kotlin:kotlin-reflect:1.2.20")
}

configurations {
    get("compile").exclude(module = "slf4j-log4j12").exclude(module = "log4j")
    get("testCompile").exclude(module = "slf4j-log4j12").exclude(module = "log4j")
}

tasks {

    val testContainerName = "kotlin-kafka-docker"

    "dockerInfo"(DockerInfo::class) {}

    "dockerVersion"(DockerVersion::class) {}

    "dockerPullImage"(DockerPullImage::class) {
        repository = "spotify/kafka"
    }

    "dockerRemoveContainer"(Exec::class) {
        group = "docker"
        executable = "docker"
        args = listOf("rm", "-f", testContainerName)
        isIgnoreExitValue = true
    }

    val dockerCreateContainer = "dockerCreateContainer"(DockerCreateContainer::class) {
        dependsOn("dockerPullImage", "dockerRemoveContainer")
        targetImageId { "spotify/kafka" }
        portBindings = listOf("2181:2181", "9092:9092")
        setEnv("ADVERTISED_HOST=127.0.0.1", "ADVERTISED_PORT=9092")
        containerName = testContainerName
    }

    "dockerStartContainer"(DockerStartContainer::class) {
        dependsOn("dockerCreateContainer")

        targetContainerId { dockerCreateContainer.containerId }
    }

    "dockerStopContainer"(DockerStopContainer::class) {
        targetContainerId { dockerCreateContainer.containerId }
    }

    "dockerWaitForContainerLog"(DockerWaitForContainerLog::class) {
        targetContainerId { dockerCreateContainer.containerId }
    }

    "test"(Test::class) {
        include("**/*Test.class")
    }

    "it"(Test::class) {
        dependsOn("test", "dockerStartContainer", "dockerWaitForContainerLog")
        //    finalizedBy("dockerStopContainer")

        include("**/*IT.class")
    }
}
