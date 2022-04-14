package com.systema.eia.iot.tb.persistence.remove

import com.systema.eia.iot.tb.persistence.search.ATbFinder
import org.springframework.web.client.HttpClientErrorException
import org.thingsboard.server.common.data.HasName
import org.thingsboard.server.common.data.id.HasId

/**
 * Implementation of ITbBaseRemover
 * need findById and remove
 * @author ViB
 */
abstract class ATbRemover<T : HasId<*>> : ITbBaseRemover<T> {
    public override fun removeIfExistById(id: String): Boolean {
        try {
            remove(id)
        } catch (e: HttpClientErrorException) {
            return false
        }
        return true
    }

    public override fun findAndRemoveById(id: String): T? {
        val obj = findById(id) ?: return null
        remove(obj.id.toString())
        return obj;
    }

    protected abstract fun findById(id: String): T?
    protected abstract fun remove(id: String)
}

/**
 * Extend base class `ATbRemover`
 * Use base finder `ATbFinder` to find by id and name
 * add function `findAndRemoveIfExistByName`
 * @author ViB
 */
abstract class ATbRemoverFinder<T : HasId<*>>(val finder: ATbFinder<T>) : ATbRemover<T>(), ITbRemoverByName<T> {
    override fun findById(id: String) = finder.getById(id)
    override fun findAndRemoveByName(name: String): T? {
        val obj = finder.getByName(name) ?: return null
        super.removeIfExistById(obj.id.toString())
        return obj;
    }
}


/**
 * Add `removeDuplicate` function, based on comparing 2 entities using `equalsObj` function
 * EqualsObj is used for comparing by name of by title in case of WidgetsBundle
 * @author ViB
 */
abstract class ATbRemoverDuplicate<T : HasId<*>>(finder: ATbFinder<T>) : ATbRemoverFinder<T>(finder),
    ITbRemoverDuplicate<T> {
    protected abstract fun equalsObj(o1: T, o2: T): Boolean

    override fun removeDuplicateById(id: String, keepOriginal: Boolean): List<T>? {
        val obj = findById(id) ?: return null
        val list = finder.getAll().filter { equalsObj(obj, it) }.toMutableList()
        if (!keepOriginal) {
            list.add(obj)
        }
        list.forEach { super.removeIfExistById(it.id.toString()) }
        return list;
    }
}


/**
 * Add `removeDuplicate` function, based on comparing 2 entities by name
 * @author ViB
 */
abstract class ATbRemoverDuplicateHasName<T>(finder: ATbFinder<T>) :
    ATbRemoverDuplicate<T>(finder) where T : HasId<*>, T : HasName {
    override fun equalsObj(o1: T, o2: T): Boolean = o1.name == o2.name
}