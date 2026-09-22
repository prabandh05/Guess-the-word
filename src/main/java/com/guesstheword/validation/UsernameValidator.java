package com.guesstheword.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    // Must have at least 5 characters, letters only, containing at least one uppercase and one lowercase letter
    private static final String USERNAME_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])[a-zA-Z]{5,}$";
    private static final Pattern PATTERN = Pattern.compile(USERNAME_PATTERN);

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null) {
            return false;
        }
        return PATTERN.matcher(username).matches();
    }
}
