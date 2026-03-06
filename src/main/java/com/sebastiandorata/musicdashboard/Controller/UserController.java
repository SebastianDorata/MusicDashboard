package com.sebastiandorata.musicdashboard.Controller;


import com.sebastiandorata.musicdashboard.Service.UserService;
import com.sebastiandorata.musicdashboard.entity.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<User> getUserByID(int userID){
        logger.info("Getting user by ID: "+ userID);

        Optional<User> userOptional = userService.getUserByID(userID);
        if(userOptional.isEmply())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        else
            return ResponseEntity.status(HttoStatus.OK).body(userOptional.get());
    }


}
