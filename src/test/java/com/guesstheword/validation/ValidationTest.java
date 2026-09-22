package com.guesstheword.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintValidatorContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ValidationTest {

    private UsernameValidator usernameValidator;
    private PasswordValidator passwordValidator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        usernameValidator = new UsernameValidator();
        passwordValidator = new PasswordValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @Test
    void testValidUsernames() {
        // At least 5 letters, letters only, containing both uppercase and lowercase
        assertTrue(usernameValidator.isValid("UserOne", context));
        assertTrue(usernameValidator.isValid("AdminUser", context));
        assertTrue(usernameValidator.isValid("aBcDe", context));
        assertTrue(usernameValidator.isValid("PlayerHero", context));
    }

    @Test
    void testInvalidUsernames() {
        // Null
        assertFalse(usernameValidator.isValid(null, context));
        // Too short (< 5)
        assertFalse(usernameValidator.isValid("Abcd", context));
        assertFalse(usernameValidator.isValid("User", context));
        // All lowercase
        assertFalse(usernameValidator.isValid("userone", context));
        // All uppercase
        assertFalse(usernameValidator.isValid("USERONE", context));
        // Contains digits
        assertFalse(usernameValidator.isValid("User1", context));
        assertFalse(usernameValidator.isValid("UserOne1", context));
        // Contains special characters
        assertFalse(usernameValidator.isValid("User$One", context));
        assertFalse(usernameValidator.isValid("User_One", context));
    }

    @Test
    void testValidPasswords() {
        // At least 5 characters, containing alpha, numeric, and one of $, %, *, &
        assertTrue(passwordValidator.isValid("Pass1$", context));
        assertTrue(passwordValidator.isValid("Pass1%", context));
        assertTrue(passwordValidator.isValid("Pass1*", context));
        assertTrue(passwordValidator.isValid("Pass1&", context));
        assertTrue(passwordValidator.isValid("Admin123*", context));
        assertTrue(passwordValidator.isValid("Player123$", context));
    }

    @Test
    void testInvalidPasswords() {
        // Null
        assertFalse(passwordValidator.isValid(null, context));
        // Too short (< 5)
        assertFalse(passwordValidator.isValid("P1$a", context));
        // No special character
        assertFalse(passwordValidator.isValid("Pass123", context));
        // Unsupported special character
        assertFalse(passwordValidator.isValid("Pass1!", context));
        assertFalse(passwordValidator.isValid("Pass1#", context));
        assertFalse(passwordValidator.isValid("Pass1@", context));
        // No digits
        assertFalse(passwordValidator.isValid("Pass$", context));
        // No letters
        assertFalse(passwordValidator.isValid("12345$", context));
    }
}
