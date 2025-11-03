package com.raketo.league.repository;

import com.raketo.league.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByTelegramId(Long telegramId);

    Optional<AdminUser> findByTelegramUsername(String telegramUsername);

    boolean existsByTelegramId(Long telegramId);
}

