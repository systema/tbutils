package com.systema.eia.iot.tb.persistence.load

import com.fasterxml.jackson.databind.JsonNode
import org.thingsboard.rest.client.RestClient
import java.io.File

abstract class AConfigUploader<T>(restClient: RestClient) : ConfigFileLoader(restClient) {
    public open fun load(pathToFile: File): T = load(read(pathToFile))
    public open fun loadAll(configDirectory: File): List<T> = readAll(configDirectory).map { load(it) }
    public open fun readAll(configDirectory: File): List<T> = readAllFiles(configDirectory).map { read(it) }
    public open fun read(pathToFile: File): T = read(readNode(pathToFile))
    public open fun load(json: JsonNode): T = load(read(json))


    public abstract fun read(json: JsonNode): T;
    public abstract fun load(entity: T): T
}