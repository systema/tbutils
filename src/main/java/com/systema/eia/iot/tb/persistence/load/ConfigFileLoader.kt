package com.systema.eia.iot.tb.persistence.load

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KotlinLogging
import org.thingsboard.rest.client.RestClient
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo
import java.io.File
import java.io.FileNotFoundException

/**
 * Wrapper for Rest client to provide config loading to TB
 * supported entity types: rule chain, customer, device, device profile, dashboard, widgets, widgets bound
 * @author ViB
 */
open class ConfigFileLoader(val restClient: RestClient) {

    companion object {
        val mapper = ObjectMapper()
    }

    private val logger = KotlinLogging.logger {}

    /**
     * Read json from a file
     * @param jsonFile path to file
     * @return read json
     * @throws FileNotFoundException, if the file doesn't exist
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws JsonParseException – if underlying input contains invalid content of type JsonParser supports (JSON
     * for default case)
     */
    fun readNode(jsonFile: File): JsonNode {
        logger.info { "Loading entity definitions from  ${jsonFile.absoluteFile}" }
        if (!jsonFile.exists()) throw FileNotFoundException("file $jsonFile does not exist!")
        return mapper.readTree(jsonFile) as ObjectNode
    }

//    protected open fun postProcessJson(res: ObjectNode) {
//        res.remove("id")
//    }


    /**
     * read all files from a dir, deserialize and then load to TB
     * @param configDirectory Root directory of configuration files
     * @param loadEntityFunction function for load entity to TB
     * @return list of deserialized entities
     * @throws IllegalArgumentException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws SecurityException If a security manager exists and its SecurityManager.checkRead(String) method denies
     * read access to the directory (see {@link File#listFiles()})
     * @throws FileNotFoundException If configDirectory is no directory
     */
    protected fun <T> loadAllFromDir(
        configDirectory: File,
        loadEntityFunction: (pathToFile: File) -> T
    ): List<T> {
        return readAllFiles(configDirectory).map { loadEntityFunction(it) }
    }

    /**
     * Read all JSON files in a directory.
     *
     * @param configDirectory - directory
     * @return list of files in the directory (may be empty, if the directory contains no JSON files)
     * @throws SecurityException If a security manager exists and its SecurityManager.checkRead(String) method denies
     * read access to the directory (see {@link File#listFiles()})
     * @throws FileNotFoundException If configDirectory is no directory
     */
    protected fun readAllFiles(configDirectory: File): List<File> {
        val files = configDirectory.listFiles()
            ?: throw FileNotFoundException("Directory $configDirectory does not exist")
//        assert(files != null && files.isNotEmpty()) { "$configDirectory is empty!" }
        return files.toList().filter { it.extension == "json" }
    }

    /**
     * Deserialize json to a entity and load it
     * @param jsonNode json config
     * @return deserialized entity
     * @throws IllegalArgumentException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */

    protected inline fun <reified T> deserializeEntity(jsonNode: JsonNode): T {
        return SearchTextBasedWithAdditionalInfo.mapper.treeToValue(jsonNode, T::class.java)
    }

    /**
     * TODO
     *
     * @param T
     * @param jsonNode
     * @param saveFunction
     * @return
     * @throws IllegalArgumentException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     */
    protected inline fun <reified T> loadEntity(
        jsonNode: JsonNode,
        saveFunction: (entity: T) -> T
    ): T {
        val entity: T = deserializeEntity(jsonNode)
        saveFunction(entity)
        return entity
    }
}
