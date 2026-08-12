package com.ist.internal_issue_tracker.issue.mapper;

import com.ist.internal_issue_tracker.issue.Issue;
import com.ist.internal_issue_tracker.issue.dto.IssueCreateRequest;
import com.ist.internal_issue_tracker.issue.dto.IssueResponse;
import com.ist.internal_issue_tracker.issue.dto.IssueUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class IssueMapper {

  /**
   * The project comes from the path and the reporter from the authenticated caller. The status is
   * left alone so the entity's {@code BACKLOG} default stands; the priority is only overwritten
   * when one was actually given, which is what makes omitting it fall back to {@code MEDIUM}.
   */
  public Issue toEntity(Integer projectId, Integer reporterId, IssueCreateRequest request) {
    Issue issue = new Issue();

    issue.setProjectId(projectId);
    issue.setReporterId(reporterId);
    issue.setName(request.name());
    issue.setDescription(request.description());
    issue.setType(request.type());
    issue.setResolvingUnit(request.resolvingUnit());
    issue.setStoryPoint(request.storyPoint());
    issue.setSprintId(request.sprintId());
    issue.setEpicId(request.epicId());
    issue.setAssigneeUserId(request.assigneeUserId());
    issue.setAssigneeTeamId(request.assigneeTeamId());

    if (request.priority() != null) {
      issue.setPriority(request.priority());
    }

    return issue;
  }

  /**
   * A replacement, so every field it owns is written even when null - omitting {@code sprintId}
   * takes the issue out of its sprint rather than leaving it where it was. Status, assignees and
   * the reporter are not this method's to touch.
   */
  public void updateEntity(Issue issue, IssueUpdateRequest request) {
    issue.setName(request.name());
    issue.setDescription(request.description());
    issue.setType(request.type());
    issue.setPriority(request.priority());
    issue.setResolvingUnit(request.resolvingUnit());
    issue.setStoryPoint(request.storyPoint());
    issue.setSprintId(request.sprintId());
    issue.setEpicId(request.epicId());
  }

  public IssueResponse toResponse(Issue issue) {
    return new IssueResponse(
        issue.getId(),
        issue.getProjectId(),
        issue.getSprintId(),
        issue.getEpicId(),
        issue.getType(),
        issue.getName(),
        issue.getDescription(),
        issue.getStatus(),
        issue.getPriority(),
        issue.getResolvingUnit(),
        issue.getStoryPoint(),
        issue.getReporterId(),
        issue.getAssigneeUserId(),
        issue.getAssigneeTeamId(),
        issue.getCreatedAt(),
        issue.getUpdatedAt());
  }
}
