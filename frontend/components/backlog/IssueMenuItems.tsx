"use client";

import { useQuery } from "@tanstack/react-query";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { useSprintsList } from "@/lib/hooks/useSprints";
import { useEpicsList } from "@/lib/hooks/useEpics";
import { useBulkIssueEdit } from "@/lib/hooks/useIssues";
import { useUserDirectory } from "@/lib/users/directory";
import { listTeams } from "@/lib/api/endpoints/teams";
import * as issues from "@/lib/api/endpoints/issues";
import type { IssueResponse } from "@/lib/api/types";

/** The Fibonacci-ish ladder every estimation session actually uses. */
const STORY_POINTS = [0, 1, 2, 3, 5, 8, 13];

/**
 * Radix ships context menus and dropdown menus as two separate component trees
 * that cannot be mixed, and this menu has to be both: right-click for speed,
 * and a toolbar button for everyone who cannot or does not right-click. So the
 * items are written once against this shape and rendered with whichever set of
 * parts the caller passes in.
 */
export type MenuParts = {
  Label: React.ComponentType<{ children?: React.ReactNode; className?: string }>;
  Item: React.ComponentType<{
    children?: React.ReactNode;
    onSelect?: (event: Event) => void;
    disabled?: boolean;
    variant?: "default" | "destructive";
  }>;
  Separator: React.ComponentType<{ className?: string }>;
  Sub: React.ComponentType<{ children?: React.ReactNode }>;
  SubTrigger: React.ComponentType<{ children?: React.ReactNode }>;
  SubContent: React.ComponentType<{ children?: React.ReactNode }>;
};

/**
 * Every field a planner adjusts from a list, applied to a whole selection at
 * once. The narrow PATCH endpoints behind these (docs/API.md §5) are what make
 * it safe in bulk — the old full-replacement PUT would have needed every issue's
 * description echoed back to avoid clearing it.
 */
export function IssueMenuItems({
  parts,
  projectId,
  targets,
  onEdit,
  onDelete,
}: {
  parts: MenuParts;
  projectId: number;
  targets: IssueResponse[];
  onEdit: (issue: IssueResponse) => void;
  onDelete: (targets: IssueResponse[]) => void;
}) {
  const { Label, Item, Separator, Sub, SubTrigger, SubContent } = parts;
  const { canManage, fieldDefinitionsByKind } = useProjectContext();
  const bulk = useBulkIssueEdit(projectId);
  const statuses = fieldDefinitionsByKind.get("ISSUE_STATUS") ?? [];
  const priorities = fieldDefinitionsByKind.get("ISSUE_PRIORITY") ?? [];
  const types = fieldDefinitionsByKind.get("ISSUE_TYPE") ?? [];

  const { data: sprints } = useSprintsList(projectId, { size: 100, sort: "startDate,desc" });
  const { data: epics } = useEpicsList(projectId, { size: 100 });
  const { byId: usersById } = useUserDirectory();
  const { data: teams } = useQuery({
    queryKey: ["teams", "picker"],
    queryFn: () => listTeams({ size: 200, sort: "name,asc" }),
  });

  const ids = targets.map((i) => i.id);
  const count = ids.length;
  const single = count === 1 ? targets[0] : undefined;
  const subject = count === 1 ? `ISS-${targets[0].id}` : `${count} issues`;

  const run = (
    apply: (issueId: number) => Promise<unknown>,
    describe: (n: number) => string,
  ) => bulk.mutate({ issueIds: ids, apply, describe });

  /** The other two fields come from each issue's own current values — a bulk
   * priority change must not level everybody's estimate to one number. */
  const reclassify = (patch: Partial<{ type: string; priority: string; storyPoint: number | null }>) =>
    (issueId: number) => {
      const issue = targets.find((i) => i.id === issueId)!;
      return issues.changeIssueClassification(projectId, issueId, {
        type: patch.type ?? issue.type,
        priority: patch.priority ?? issue.priority,
        storyPoint: patch.storyPoint !== undefined ? patch.storyPoint : issue.storyPoint,
      });
    };

  const users = [...usersById.values()].filter((u) => u.isActive);
  const activeTeams = (teams?.content ?? []).filter((t) => t.isActive);

  return (
    <>
      <Label>{subject}</Label>

      <Sub>
        <SubTrigger>Sprint</SubTrigger>
        <SubContent>
          <Item
            onSelect={() =>
              run(
                (id) => issues.changeIssueSprint(projectId, id, { sprintId: null }),
                (n) => `${n === 1 ? "Issue" : `${n} issues`} moved to the backlog.`,
              )
            }
          >
            Backlog (no sprint)
          </Item>
          <Separator />
          {sprints?.content.length ? (
            sprints.content.map((s) => (
              <Item
                key={s.id}
                onSelect={() =>
                  run(
                    (id) => issues.changeIssueSprint(projectId, id, { sprintId: s.id }),
                    (n) => `${n === 1 ? "Issue" : `${n} issues`} moved to ${s.name}.`,
                  )
                }
              >
                {s.name}
              </Item>
            ))
          ) : (
            <Item disabled>No sprints in this project</Item>
          )}
        </SubContent>
      </Sub>

      <Sub>
        <SubTrigger>Epic</SubTrigger>
        <SubContent>
          <Item
            onSelect={() =>
              run(
                (id) => issues.changeIssueEpic(projectId, id, { epicId: null }),
                (n) => `${n === 1 ? "Issue" : `${n} issues`} removed from their epic.`,
              )
            }
          >
            No epic
          </Item>
          <Separator />
          {epics?.content.length ? (
            epics.content.map((e) => (
              <Item
                key={e.id}
                onSelect={() =>
                  run(
                    (id) => issues.changeIssueEpic(projectId, id, { epicId: e.id }),
                    (n) => `${n === 1 ? "Issue" : `${n} issues`} moved to ${e.name}.`,
                  )
                }
              >
                {e.name}
              </Item>
            ))
          ) : (
            <Item disabled>No epics in this project</Item>
          )}
        </SubContent>
      </Sub>

      <Sub>
        <SubTrigger>Assignee</SubTrigger>
        <SubContent>
          <Item
            onSelect={() =>
              run(
                (id) => issues.clearIssueAssignee(projectId, id),
                (n) => `${n === 1 ? "Issue" : `${n} issues`} unassigned.`,
              )
            }
          >
            Unassign
          </Item>
          <Separator />
          {users.map((u) => (
            <Item
              key={u.id}
              onSelect={() =>
                run(
                  (id) =>
                    issues.changeIssueAssignee(projectId, id, {
                      assigneeUserId: u.id,
                      assigneeTeamId: null,
                    }),
                  (n) => `${n === 1 ? "Issue" : `${n} issues`} assigned to ${u.name} ${u.surname}.`,
                )
              }
            >
              {u.name} {u.surname}
            </Item>
          ))}
          {activeTeams.length > 0 && <Separator />}
          {activeTeams.map((t) => (
            <Item
              key={`team-${t.id}`}
              onSelect={() =>
                run(
                  (id) =>
                    issues.changeIssueAssignee(projectId, id, {
                      assigneeUserId: null,
                      assigneeTeamId: t.id,
                    }),
                  (n) => `${n === 1 ? "Issue" : `${n} issues`} assigned to ${t.name}.`,
                )
              }
            >
              {t.name} (team)
            </Item>
          ))}
        </SubContent>
      </Sub>

      <Sub>
        <SubTrigger>Status</SubTrigger>
        <SubContent>
          {statuses.map((s) => (
            <Item
              key={s.code}
              onSelect={() =>
                run(
                  (id) => issues.changeIssueStatus(projectId, id, { status: s.code }),
                  (n) => `${n === 1 ? "Issue" : `${n} issues`} moved to ${s.label}.`,
                )
              }
            >
              {s.label}
            </Item>
          ))}
        </SubContent>
      </Sub>

      <Sub>
        <SubTrigger>Priority</SubTrigger>
        <SubContent>
          {priorities.map((p) => (
            <Item
              key={p.code}
              onSelect={() =>
                run(
                  reclassify({ priority: p.code }),
                  (n) => `${n === 1 ? "Issue" : `${n} issues`} set to ${p.label}.`,
                )
              }
            >
              {p.label}
            </Item>
          ))}
        </SubContent>
      </Sub>

      <Sub>
        <SubTrigger>Type</SubTrigger>
        <SubContent>
          {types.map((t) => (
            <Item
              key={t.code}
              onSelect={() =>
                run(
                  reclassify({ type: t.code }),
                  (n) => `${n === 1 ? "Issue" : `${n} issues`} set to ${t.label}.`,
                )
              }
            >
              {t.label}
            </Item>
          ))}
        </SubContent>
      </Sub>

      <Sub>
        <SubTrigger>Story points</SubTrigger>
        <SubContent>
          {STORY_POINTS.map((p) => (
            <Item
              key={p}
              onSelect={() =>
                run(
                  reclassify({ storyPoint: p }),
                  (n) => `${n === 1 ? "Issue" : `${n} issues`} estimated at ${p}.`,
                )
              }
            >
              {p}
            </Item>
          ))}
          <Separator />
          <Item
            onSelect={() =>
              run(
                reclassify({ storyPoint: null }),
                (n) => `${n === 1 ? "Issue" : `${n} issues`} left unestimated.`,
              )
            }
          >
            Clear
          </Item>
        </SubContent>
      </Sub>

      <Separator />

      <Item disabled={!single} onSelect={() => single && onEdit(single)}>
        Edit…
      </Item>
      {canManage && (
        <Item variant="destructive" onSelect={() => onDelete(targets)}>
          Delete
        </Item>
      )}
    </>
  );
}
