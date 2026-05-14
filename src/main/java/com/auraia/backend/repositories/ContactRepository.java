package com.auraia.backend.repositories;

import com.auraia.backend.models.entities.Contact;
import com.auraia.backend.models.entities.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    List<Contact> findByUserOrderByPriorityAscNameAsc(User user);

    List<Contact> findByUserAndSosEnabledTrueOrderByPriorityAsc(User user);

    Optional<Contact> findByIdAndUser(UUID id, User user);

    long countByUserAndSosEnabledTrueAndAvailableTrue(User user);

    void deleteAllByUser(User user);
}
