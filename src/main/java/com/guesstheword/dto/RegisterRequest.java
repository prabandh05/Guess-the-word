package com.guesstheword.dto;

import com.guesstheword.entity.Role;
import com.guesstheword.validation.ValidPassword;
import com.guesstheword.validation.ValidUsername;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @ValidUsername
    private String username;

    @ValidPassword
    private String password;

    private Role role;
}
