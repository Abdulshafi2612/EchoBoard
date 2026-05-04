package com.echoboard.service.impl;

import com.echoboard.entity.User;
import com.echoboard.security.CustomUserDetails;
import com.echoboard.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserServiceImpl  implements CurrentUserService {
    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        return currentUser.getUser();
    }
}
