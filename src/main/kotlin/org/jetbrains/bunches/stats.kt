@file:Suppress("PackageDirectoryMismatch")
@file:JvmName("BunchStats")
package org.jetbrains.bunches.stats

import org.jetbrains.bunches.check.isDeletedBunchFile
import org.jetbrains.bunches.file.readExtensionFromFile
import org.jetbrains.bunches.general.exitWithError
import org.jetbrains.bunches.general.exitWithUsageError
import org.jetbrains.bunches.restore.isGitDir
import org.jetbrains.bunches.restore.isGradleBuildDir
import org.jetbrains.bunches.restore.isGradleDir
import org.jetbrains.bunches.restore.isOutDir
import java.io.File

data class Settings(val repoPath: String)

fun main(args: Array<String>) {
    stats(args)
}

const val STATS_DESCRIPTION = "Show statistics about bunch files in repository."

fun stats(args: Array<String>) {
    if (args.size !in 1..1) {
        exitWithUsageError("""
            Usage: <git-path>

            $STATS_DESCRIPTION

            <git-path>   - Directory with repository (parent directory for .git).

            Example:
            bunch stats C:/Projects/kotlin
            """.trimIndent())
    }

    val settings = Settings(args[0])

    doStats(settings)
}

fun doStats(settings: Settings) {
    val extensions = readExtensionFromFile(settings.repoPath) ?: exitWithError()

    val root = File(settings.repoPath)
    val bunchFiles = root
            .walkTopDown()
            .onEnter { dir -> !(isGitDir(dir) || isOutDir(dir, root) || isGradleBuildDir(dir) || isGradleDir(dir)) }
            .filter { child -> child.extension in extensions }
            .toList()

    val groupedFiles = bunchFiles.groupBy { it.extension }

    val affectedOriginFiles: Set<File> =
            bunchFiles.mapTo(HashSet()) { child -> File(child.parentFile, child.nameWithoutExtension) }

    println("Number of affected origin files: ${affectedOriginFiles.size}")
    println()

    println("${"%-6s".format("Ext")}|${"%6s".format("exists")} |${"%6s".format("del")} |${"%6s".format("total")}")

    val all = bunchFiles.size
    val allDeleted = bunchFiles.count { isDeletedBunchFile(it) }
    print("%-6s".format("all"))
    print("|${"%6d".format(all - allDeleted)} ")
    print("|${"%6d".format(allDeleted)} ")
    print("|${"%6d".format(all)}")
    println()

    for (extension in extensions) {
        val currentExtensionFiles = groupedFiles[extension] ?: listOf()
        val deleted = currentExtensionFiles.count { isDeletedBunchFile(it) }
        val total = currentExtensionFiles.size

        print("%-6s".format(extension))
        print("|${"%6d".format(total - deleted)} ")
        print("|${"%6d".format(deleted)} ")
        print("|${"%6d".format(total)}")
        println()
    }
}
