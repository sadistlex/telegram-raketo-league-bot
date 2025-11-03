package com.raketo.league.service;

import com.raketo.league.model.AdminUser;
import com.raketo.league.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminUserRepository adminUserRepository;

    @Transactional(readOnly = true)
    public boolean isAdmin(Long telegramId) {
        return adminUserRepository.existsByTelegramId(telegramId);
    }

    @Transactional(readOnly = true)
    public Optional<AdminUser> findByTelegramId(Long telegramId) {
        return adminUserRepository.findByTelegramId(telegramId);
    }

    @Transactional
    public AdminUser createAdmin(Long telegramId, String username, String firstName, String lastName) {
        AdminUser admin = AdminUser.builder()
                .telegramId(telegramId)
                .telegramUsername(username)
                .firstName(firstName)
                .lastName(lastName)
                .isActive(true)
                .build();
        return adminUserRepository.save(admin);
    }
}

