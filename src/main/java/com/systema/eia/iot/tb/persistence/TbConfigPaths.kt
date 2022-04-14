package com.systema.eia.iot.tb.persistence

import java.io.File

class TbConfigPaths(configRootDir: File) {
    init {
        if (!configRootDir.isDirectory) {
            throw RuntimeException("TB configuration directory $configRootDir does not exist")
        }
    }

    val rootDir = configRootDir

    @JvmField
    val dashboardsPath = File(rootDir, "dashboards")

    @JvmField
    val rulesPath = File(rootDir, "rules")

    @JvmField
    val profilesPath = File(rootDir, "profiles")

    @JvmField
    val assetsPath = File(rootDir, "assets")

    @JvmField
    val customersPath = File(rootDir, "customers")

    @JvmField
    val tenantsPath = File(rootDir, "tenants")

    @JvmField
    val relationsPath = File(rootDir, "relations")

    @JvmField
    val widgetsPath = File(rootDir, "widgets")

    @JvmField
    val devicesPath = File(rootDir, "devices")

    @JvmField
    val widgetsBundlePath = File(rootDir, "bundles")
}

/** Substitute all occurrences of variable mapping in an input file and save the result to an output file.*/
fun File.substituteVars(output: File, params: Map<String, String>) {
    output.printWriter().use { out ->
        useLines { input ->
            input.iterator().forEach { line ->
                val fold = params.toList().fold(line) { acc, (param, value) -> acc.replace(param, value) }
                out.println(fold)
            }
        }
    }
}
