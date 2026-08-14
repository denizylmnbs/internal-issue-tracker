package com.ist.internal_issue_tracker.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.ist.internal_issue_tracker.issue.dto.ChangeSprintRequest;
import com.ist.internal_issue_tracker.issue.mapper.IssueMapper;
import com.ist.internal_issue_tracker.shared.port.EpicLookup;
import com.ist.internal_issue_tracker.shared.port.FieldDefinitionLookup;
import com.ist.internal_issue_tracker.shared.port.FieldKind;
import com.ist.internal_issue_tracker.shared.port.FieldSemantic;
import com.ist.internal_issue_tracker.shared.port.ProjectLookup;
import com.ist.internal_issue_tracker.shared.port.SprintLookup;
import com.ist.internal_issue_tracker.shared.port.TeamLookup;
import com.ist.internal_issue_tracker.shared.port.UserLookup;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Covers what {@code changeSprint} does to the status, which is the one field it touches beyond the
 * sprint itself - see {@code IssueService#returnToBacklog}. The rest of the service is exercised by
 * hand against a running database, for the reason recorded in the project's test notes.
 */
@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

  private static final Integer PROJECT_ID = 1;
  private static final Integer ISSUE_ID = 7;
  private static final Integer ACTOR_ID = 3;
  private static final Integer SPRINT_ID = 42;

  @Mock private IssueRepository issueRepository;
  @Mock private IssueMapper issueMapper;
  @Mock private IssueChangeDetector issueChangeDetector;
  @Mock private ProjectLookup projectLookup;
  @Mock private SprintLookup sprintLookup;
  @Mock private EpicLookup epicLookup;
  @Mock private UserLookup userLookup;
  @Mock private TeamLookup teamLookup;
  @Mock private FieldDefinitionLookup fieldDefinitionLookup;
  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private IssueService issueService;

  /** Set by each test before the service runs; the repository stub hands this one back. */
  private Issue issue;

  private static Issue issueIn(Integer sprintId, String status) {
    Issue issue = new Issue();
    issue.setId(ISSUE_ID);
    issue.setProjectId(PROJECT_ID);
    issue.setSprintId(sprintId);
    issue.setStatus(status);
    return issue;
  }

  @BeforeEach
  void stubCommonPath() {
    lenient().when(projectLookup.existsActiveProject(PROJECT_ID)).thenReturn(true);
    lenient()
        .when(issueRepository.findByIdAndProjectIdAndDeletedAtIsNull(ISSUE_ID, PROJECT_ID))
        .thenAnswer(invocation -> Optional.of(issue));
    lenient().when(issueRepository.save(any(Issue.class))).thenAnswer(i -> i.getArgument(0));
    lenient()
        .when(issueChangeDetector.diff(any(), any()))
        .thenReturn(java.util.Collections.emptyList());
  }

  private void stubStatusSemantics() {
    when(fieldDefinitionLookup.codesWithSemantic(
            PROJECT_ID, FieldKind.ISSUE_STATUS, FieldSemantic.DONE))
        .thenReturn(Set.of("DONE"));
    when(fieldDefinitionLookup.codesWithSemantic(
            PROJECT_ID, FieldKind.ISSUE_STATUS, FieldSemantic.CANCELLED))
        .thenReturn(Set.of("CANCELLED"));
  }

  @Test
  void changeSprint_movesStatusToTheDefault_whenTheIssueLeavesItsSprint() {
    issue = issueIn(SPRINT_ID, "IN_PROGRESS");
    stubStatusSemantics();
    when(fieldDefinitionLookup.defaultCode(PROJECT_ID, FieldKind.ISSUE_STATUS))
        .thenReturn("BACKLOG");

    issueService.changeSprint(PROJECT_ID, ISSUE_ID, ACTOR_ID, new ChangeSprintRequest(null));

    assertThat(issue.getSprintId()).isNull();
    assertThat(issue.getStatus()).isEqualTo("BACKLOG");
  }

  @Test
  void changeSprint_keepsTheStatus_whenTheIssueLeavingItsSprintIsAlreadyDone() {
    issue = issueIn(SPRINT_ID, "DONE");
    stubStatusSemantics();

    issueService.changeSprint(PROJECT_ID, ISSUE_ID, ACTOR_ID, new ChangeSprintRequest(null));

    assertThat(issue.getSprintId()).isNull();
    assertThat(issue.getStatus()).isEqualTo("DONE");
  }

  @Test
  void changeSprint_keepsTheStatus_whenTheIssueWasAlreadyOutOfEverySprint() {
    issue = issueIn(null, "TODO");

    issueService.changeSprint(PROJECT_ID, ISSUE_ID, ACTOR_ID, new ChangeSprintRequest(null));

    assertThat(issue.getStatus()).isEqualTo("TODO");
  }

  @Test
  void changeSprint_keepsTheStatus_whenTheIssueIsMovedIntoASprint() {
    issue = issueIn(null, "TODO");
    when(sprintLookup.existsLiveSprintInProject(PROJECT_ID, SPRINT_ID)).thenReturn(true);

    issueService.changeSprint(PROJECT_ID, ISSUE_ID, ACTOR_ID, new ChangeSprintRequest(SPRINT_ID));

    assertThat(issue.getSprintId()).isEqualTo(SPRINT_ID);
    assertThat(issue.getStatus()).isEqualTo("TODO");
  }
}
