package com.ist.internal_issue_tracker.user;

import com.ist.internal_issue_tracker.shared.security.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserRepository extends JpaRepository<User, Integer> {
  Boolean existsByEmail(String email);

  Boolean existsByEmailAndIdNot(String email, Integer id);

  Optional<User> findByEmail(String email);

  boolean existsByIdAndIsActiveTrue(Integer id);

  /**
   * The {@code CAST(... AS String)} wrappers are load-bearing: a bare {@code :name IS NULL} gives
   * Hibernate no context to infer the parameter type from, so the driver sends it untyped and
   * PostgreSQL fails the whole statement with {@code function lower(bytea) does not exist}. Casting
   * pins the parameter to varchar at every bind site.
   */
  @Query(
      """
              SELECT u FROM User u
              WHERE (CAST(:name AS String) IS NULL
                     OR lower(u.name) LIKE lower(concat('%', CAST(:name AS String), '%')))
              AND (CAST(:surname AS String) IS NULL
                   OR lower(u.surname) LIKE lower(concat('%', CAST(:surname AS String), '%')))
              """)
  Page<User> findAllByFilters(
      @Param("name") String name, @Param("surname") String surname, Pageable pageable);

  @Query("SELECT u.role FROM User u WHERE u.id = :id AND u.isActive = true")
  Optional<Role> findActiveRoleById(@Param("id") Integer id);
}
