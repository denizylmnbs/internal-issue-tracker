package com.ist.internal_issue_tracker.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {

  Page<TeamMember> findAllByTeamId(Integer teamId, Pageable pageable);
}
