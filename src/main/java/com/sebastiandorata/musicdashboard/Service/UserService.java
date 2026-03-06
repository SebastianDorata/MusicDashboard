package com.sebastiandorata.musicdashboard.Service;


import com.sebastiandorata.musicdashboard.DataAccess.UserRepository;
import com.sebastiandorata.musicdashboard.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.logging.Logger;

@Service
public class UserService {
    private static final Logger logger = Logger.getLogger(UserService.class.getName());

    @Autowired //Use a copy of the class so I don't have to create it myslef
    private UserRepository userRepository;

    public Optional<User> getUserByID(int userID){
        logger.info("Getting the user by id: "+ userID);
        return userRepository.findById(userID);
    }
}
