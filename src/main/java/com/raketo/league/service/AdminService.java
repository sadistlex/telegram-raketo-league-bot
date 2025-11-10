package com.raketo.league.service;

import com.raketo.league.model.AdminUser;
import com.raketo.league.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

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

