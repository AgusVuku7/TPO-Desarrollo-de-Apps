package com.chargeroute.app

import com.chargeroute.app.domain.model.KnownCities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KnownCitiesTest {

    @Test
    fun exactNameFindsTheCity() {
        assertEquals("Tandil", KnownCities.findByName("Tandil")?.name)
    }

    @Test
    fun searchIgnoresUpperAndLowerCase() {
        assertEquals("Rosario", KnownCities.findByName("rOsArIo")?.name)
    }

    @Test
    fun partialTextFindsTheCity() {
        assertEquals("Villa Gesell", KnownCities.findByName("Gesell")?.name)
    }

    @Test
    fun unknownTextReturnsNull() {
        assertNull(KnownCities.findByName("Ushuaia"))
    }

    @Test
    fun emptyTextReturnsTheFirstCity() {
        // Comportamiento actual: "" esta contenido en cualquier nombre, asi que devuelve el Obelisco (el primero)
        assertEquals(KnownCities.OBELISCO, KnownCities.findByName(""))
    }

    @Test
    fun ambiguousTextReturnsTheFirstOneInTheList() {
        // Comportamiento actual: "Plata" esta en "Mar del Plata" y en "La Plata"; gana el primero de la lista
        assertEquals("Mar del Plata", KnownCities.findByName("Plata")?.name)
    }

    @Test
    fun textWithoutAccentDoesNotMatchAccentedName() {
        // Comportamiento actual: "Cordoba" no encuentra "Córdoba Capital" (no se ignoran las tildes)
        assertNull(KnownCities.findByName("Cordoba"))
    }
}
