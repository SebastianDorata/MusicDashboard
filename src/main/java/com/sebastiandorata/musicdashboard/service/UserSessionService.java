package com.sebastiandorata.musicdashboard.service;

import com.sebastiandorata.musicdashboard.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Setter
@Getter
@Service
public class UserSessionService {

    private User currentUser;

    public Long getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }
}