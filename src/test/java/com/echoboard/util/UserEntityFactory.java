package com.echoboard.util;

import com.echoboard.entity.User;
import com.echoboard.enums.UserRole;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
public final class UserEntityFactory {


    public static User presenter() {
        User user = new User();

        user.setId(1L);
        user.setName("Test Presenter");
        user.setEmail("presenter@example.com");
        user.setPasswordHash("encoded-password");
        user.setRole(UserRole.PRESENTER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setUpdatedAt(LocalDateTime.now());

        return user;
    }

    public static User admin() {
        User user = presenter();

        user.setId(2L);
        user.setName("Test Admin");
        user.setEmail("admin@example.com");
        user.setRole(UserRole.ADMIN);

        return user;
    }

    public static User disabledPresenter() {
        User user = presenter();

        user.setEnabled(false);

        return user;
    }
}