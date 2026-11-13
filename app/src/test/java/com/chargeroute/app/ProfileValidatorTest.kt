package com.chargeroute.app

import com.chargeroute.app.domain.model.ProfileValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidatorTest {

    @Test
    fun nameWithTextIsValid() {
        assertTrue(ProfileValidator.isValidName("Sofía Martín"))
    }

    @Test
    fun emptyOrBlankNameIsNotValid() {
        assertFalse(ProfileValidator.isValidName(""))
        assertFalse(ProfileValidator.isValidName("   "))
    }

    @Test
    fun wellFormedEmailIsValid() {
        assertTrue(ProfileValidator.isValidEmail("sofia.martin@gmail.com"))
        // Los espacios de los costados no cuentan
        assertTrue(ProfileValidator.isValidEmail("  sofia@uade.edu.ar "))
    }

    @Test
    fun emailWithoutAtOrDomainIsNotValid() {
        assertFalse(ProfileValidator.isValidEmail(""))
        assertFalse(ProfileValidator.isValidEmail("sofia"))
        assertFalse(ProfileValidator.isValidEmail("sofia@gmail"))
        assertFalse(ProfileValidator.isValidEmail("@gmail.com"))
        assertFalse(ProfileValidator.isValidEmail("sofia martin@gmail.com"))
    }
}
