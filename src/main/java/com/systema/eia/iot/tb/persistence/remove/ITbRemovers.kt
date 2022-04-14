package com.systema.eia.iot.tb.persistence.remove

import org.thingsboard.server.common.data.id.HasId

/**
 * Basic remover interface
 */
interface ITbBaseRemover<T : HasId<*>> {
    /**
     * find object (using TBFinder) und remove it if exist
     * @param id entity id as string
     * @return found Object, else null
     */
    public fun findAndRemoveById(id: String): T?


    /**
     * try to remove object
     * @param id entity id as string
     * @return true if object exist and has been removed, else false
     */
    public fun removeIfExistById(id: String): Boolean
}


/**
 * add findAndRemoveByName function
 */
interface ITbRemoverByName<T : HasId<*>> {
    /**
     * find entity using finder and remove if it has been found
     * @return found Object, else null
     */
    fun findAndRemoveByName(name: String): T?

}


/**
 * add removeDuplicateById function
 */
interface ITbRemoverDuplicate<T : HasId<*>> {
    /**
     * search entity with the same fields (name, title)
     * and remove they
     * @param id id of original entity
     * @param keepOriginal if true -> does not delete the original, else it deletes the original, even the duplicates were not found.
     * @return found Object, else null
     */
    fun removeDuplicateById(id: String, keepOriginal: Boolean): List<T>?
}