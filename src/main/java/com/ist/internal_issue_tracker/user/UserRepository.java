package com.ist.internal_issue_tracker.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Integer> {
    Boolean existsByEmail(String email);

    Boolean existsByEmailAndIdNot(String email, Integer id);

    @Query("""
            SELECT u FROM User u
            WHERE (:name IS NULL OR lower(u.name) LIKE lower(concat('%', :name, '%')))
            AND (:surname IS NULL OR lower(u.surname) LIKE lower(concat('%', :surname, '%')))
            """)
    Page<User> findAllByFilters(
            @Param("name") String name,
            @Param("surname") String surname,
            Pageable pageable
    );
}
