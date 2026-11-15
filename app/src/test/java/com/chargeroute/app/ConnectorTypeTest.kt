package com.chargeroute.app

import com.chargeroute.app.domain.model.ConnectorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun unknownTextReturnsNull() {
        // Un texto que no es de ningun conector conocido no se convierte en CCS2
        assertNull(ConnectorType.fromString("GB/T"))
    }

    @Test
    fun emptyTextReturnsNull() {
        // "" esta contenido en todos los displayName, pero un texto vacio no es un conector
        assertNull(ConnectorType.fromString(""))
    }

    @Test
    fun partialTextMatchesTheFirstTypeThatContainsIt() {
        // "CCS" es parte de "CCS2" y "CCS1": devuelve CCS2 porque es el primero de la lista
        assertEquals(ConnectorType.CCS2, ConnectorType.fromString("CCS"))
    }

    @Test
    fun dcAndAcConnectors() {
        val dc = listOf(ConnectorType.CCS2, ConnectorType.CHADEMO, ConnectorType.CCS1, ConnectorType.TESLA)
        val ac = listOf(ConnectorType.TYPE_2, ConnectorType.SCHUKO)
        for (type in dc) assertTrue(type.name, type.isDc)
        for (type in ac) assertFalse(type.name, type.isDc)
    }
}
