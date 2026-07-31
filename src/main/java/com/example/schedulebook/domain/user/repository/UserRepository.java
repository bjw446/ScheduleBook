package com.example.schedulebook.domain.user.repository;

import com.example.schedulebook.domain.user.entity.User;
import com.example.schedulebook.domain.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginId(String loginId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.userRole = :userRole AND u.deleted = false " +
            "AND u.userStatus = com.example.schedulebook.domain.user.enums.UserStatus.ACTIVE")
    List<User> findAllActiveAdmins(@Param("userRole") UserRole userRole);
}
