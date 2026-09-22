package com.guesstheword.config;

import com.guesstheword.entity.Role;
import com.guesstheword.entity.User;
import com.guesstheword.entity.Word;
import com.guesstheword.repository.UserRepository;
import com.guesstheword.repository.WordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SeedDataTest {

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testInitialWordsSeededCorrectly() {
        List<Word> words = wordRepository.findAll();
        assertEquals(20, words.size(), "Should have exactly 20 words seeded in database");

        for (Word word : words) {
            assertEquals(5, word.getText().length(), "Each word must be exactly 5 letters");
            assertEquals(word.getText().toUpperCase(), word.getText(), "Each word must be uppercase");
        }
    }

    @Test
    void testInitialUsersSeededCorrectly() {
        Optional<User> adminOpt = userRepository.findByUsername("AdminUser");
        assertTrue(adminOpt.isPresent(), "Admin user 'AdminUser' should be present");
        assertEquals(Role.ADMIN, adminOpt.get().getRole(), "Admin user should have ADMIN role");

        Optional<User> playerOpt = userRepository.findByUsername("PlayerOne");
        assertTrue(playerOpt.isPresent(), "Player user 'PlayerOne' should be present");
        assertEquals(Role.PLAYER, playerOpt.get().getRole(), "Player user should have PLAYER role");
    }
}
