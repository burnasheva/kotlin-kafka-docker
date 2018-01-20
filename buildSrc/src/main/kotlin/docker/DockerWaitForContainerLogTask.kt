import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.io.IOException
import java.util.concurrent.TimeUnit

open class DockerWaitForContainerLogTask : DefaultTask() {

    init {
        group = "Docker"
        description = "waits until container logs a given message."
    }

    @TaskAction
    fun run() {
        val timeout = System.currentTimeMillis() + this.timeout
        var started = false
        while (!started && System.currentTimeMillis() < timeout) {
            val dockerLogOutput = "docker logs $containerId".runShell() ?: continue

            if (Regex(expectedLog).containsMatchIn(dockerLogOutput)) {
                logger.lifecycle("found log '$expectedLog' in container log from ID '$containerId'.")
                started = true
            }

            Thread.sleep(1000)
        }

        if (!started) {
            throw RuntimeException("did not find log '$expectedLog' in container log from ID '$containerId' within $timeout ms!")
        }
    }

    @get:Input
    val containerId by lazy { targetContainerId() }

    @get:Input
    var expectedLog = "kafka entered RUNNING .*"

    @get:Input
    var timeout = 30_000

    var targetContainerId: () -> String? = { null }

    fun targetContainerId(lookup: () -> String?) {
        targetContainerId = lookup
    }

    private fun String.runShell(): String? {
        try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(5, TimeUnit.SECONDS)
            return proc.inputStream.bufferedReader().readText()
        } catch (e: IOException) {
            logger.warn("failed to invoke docker command", e)
            return ""
        }
    }
}
