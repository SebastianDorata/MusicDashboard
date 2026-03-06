package com.sebastiandorata.musicdashboard.DataAccess;

import com.sebastiandorata.musicdashboard.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository <User, Integer>{
}
