package com.chargeroute.app

import com.chargeroute.app.domain.model.ConnectorType
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectorTypeTest {

    @Test
    fun everyEnumNameComesBackAsItsType() {
        for (type in ConnectorType.entries) {
            assertEquals(type, ConnectorType.fromString(type.name))
        }
    }

    @Test
    fun everyDisplayNameComesBackAsItsType() {
        for (type in ConnectorType.entries) {
            assertEquals(type, ConnectorType.fromString(type.displayName))
        }
    }

    @Test
    fun knownNamesAreMatchedIgnoringCase() {
        assertEquals(ConnectorType.CHADEMO, ConnectorType.fromString("chademo"))
        assertEquals(ConnectorType.TYPE_2, ConnectorType.fromString("type_2"))
        assertEquals(ConnectorType.TESLA, ConnectorType.fromString("tesla"))
        assertEquals(ConnectorType.CCS1, ConnectorType.fromString("ccs1"))
    }

    @Test
    fun unknownTextFallsBackToCcs2() {
        // Comportamiento actual: un texto desconocido no falla, devuelve CCS2
        assertEquals(ConnectorType.CCS2, ConnectorType.fromString("GB/T"))
    }

    @Test
    fun emptyTextReturnsCcs2() {
        // Comportamiento actual: "" esta contenido en todos los displayName, asi que gana el primero (CCS2)
        assertEquals(ConnectorType.CCS2, ConnectorType.fromString(""))
    }

    @Test
    fun partialTextMatchesTheFirstTypeThatContainsIt() {
        // Comportamiento actual: "CCS" es parte de "CCS2" y "CCS1", y devuelve CCS2 (el primero de la lista)
        assertEquals(ConnectorType.CCS2, ConnectorType.fromString("CCS"))
    }
}
