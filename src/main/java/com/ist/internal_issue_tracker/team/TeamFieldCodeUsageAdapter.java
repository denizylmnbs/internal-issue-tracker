package com.ist.internal_issue_tracker.team;

import com.ist.internal_issue_tracker.shared.port.FieldCodeUsageResolver;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Owns {@link FieldKind#TEAM_FIELD} - global, so {@code projectId} is ignored throughout,
 * matching every other port here. */
@Component
@RequiredArgsConstructor
class TeamFieldCodeUsageAdapter implements FieldCodeUsageResolver {

  private final TeamRepository repository;

  @Override
  public boolean supports(FieldKind kind) {
    return kind == FieldKind.TEAM_FIELD;
  }

  @Override
  @Transactional(readOnly = true)
  public long countUsages(FieldKind kind, Integer projectId, String code) {
    return repository.countByFieldAndIsActiveTrue(code);
  }

  @Override
  @Transactional
  public void reassign(FieldKind kind, Integer projectId, String fromCode, String toCode) {
    List<Team> rows = repository.findByFieldAndIsActiveTrue(fromCode);
    rows.forEach(team -> team.setField(toCode));
    repository.saveAll(rows);
  }
}
