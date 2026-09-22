package com.guesstheword.config;

import com.guesstheword.entity.Role;
import com.guesstheword.entity.User;
import com.guesstheword.entity.Word;
import com.guesstheword.repository.UserRepository;
import com.guesstheword.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final WordRepository wordRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static final List<String> INITIAL_WORDS = Arrays.asList(
            "APPLE", "BEACH", "CRANE", "DREAM", "EAGLE",
            "FLAME", "GRAPE", "HOUSE", "LIGHT", "MUSIC",
            "OCEAN", "PIANO", "QUEEN", "RIVER", "SHINE",
            "TIGER", "WATER", "WORLD", "YOUTH", "ZEBRA"
    );

    @Override
    public void run(String... args) {
        seedWords();
        seedUsers();
    }

    private void seedWords() {
        if (wordRepository.count() == 0) {
            log.info("Seeding initial 20 five-letter uppercase words into database...");
            for (String wordText : INITIAL_WORDS) {
                wordRepository.save(Word.builder()
                        .text(wordText.toUpperCase())
                        .build());
            }
            log.info("Successfully seeded {} words.", INITIAL_WORDS.size());
        }
    }

    private void seedUsers() {
        // Seed default Admin user
        if (!userRepository.existsByUsername("AdminUser")) {
            log.info("Seeding default ADMIN user: AdminUser...");
            User admin = User.builder()
                    .username("AdminUser")
                    .password(passwordEncoder.encode("Admin123*"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Admin user 'AdminUser' seeded successfully.");
        }

        // Seed default Player user for testing
        if (!userRepository.existsByUsername("PlayerOne")) {
            log.info("Seeding default PLAYER user: PlayerOne...");
            User player = User.builder()
                    .username("PlayerOne")
                    .password(passwordEncoder.encode("Player123$"))
                    .role(Role.PLAYER)
                    .build();
            userRepository.save(player);
            log.info("Player user 'PlayerOne' seeded successfully.");
        }
    }
}
