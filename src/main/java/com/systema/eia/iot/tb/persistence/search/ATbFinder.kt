package com.systema.eia.iot.tb.persistence.search

interface ATbFinder<T> {
    public fun getById(id: String): T?
    public fun getByName(name: String): T?
    public fun getAll(): List<T>
}