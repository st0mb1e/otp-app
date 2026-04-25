package ru.yartsev_vladislav.otp_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yartsev_vladislav.otp_app.domain.Role;
import ru.yartsev_vladislav.otp_app.domain.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByRole(Role role);

    List<User> findAllByRoleNot(Role role);
}
