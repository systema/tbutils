package com.systema.eia.iot.tb.utils


public enum class RelationDirection {
    FROM, TO, BOTH;

    fun isFrom(): Boolean {
        return this != TO
    }

    fun isTo(): Boolean {
        return this != FROM
    }
}