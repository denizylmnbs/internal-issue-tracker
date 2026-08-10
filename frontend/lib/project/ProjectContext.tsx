"use client";

import { createContext, useContext, useMemo } from "react";
import { useSession } from "@/lib/auth/session";
import { useProject, useProjectParticipants } from "@/lib/hooks/useProjects";
import {
  editorLeaderOrParticipant,
  editorOrProjectLeader,
  isEditorOrAbove,
} from "@/lib/auth/can";
import type { ProjectDetailResponse } from "@/lib/api/types";

/**
 * One project + participant fetch per project, shared by every page under
 * /projects/[id] — rather than each board/backlog/sprint/epic page re-asking
 * "is the caller a participant here?" Backed by GET /participants (never
 * /members — docs/API.md §4.6), which is also what feeds the leader/
 * participant checks below.
 */
type ProjectContextValue = {
  projectId: number;
  project: ProjectDetailResponse | undefined;
  isLoading: boolean;
  participantIds: Set<number>;
  isLeader: boolean;
  isParticipant: boolean;
  /** editor / leader — planning artifacts (sprints, epics), destructive issue ops. */
  canManage: boolean;
  /**
   * editor / leader / participant — issue creation, full-edit, comment writes, activity, metrics.
   * NOT status or assignee changes: those are narrower (editor / leader / the issue's own
   * assignee) and need a per-issue check — see `lib/auth/can.ts#canWriteIssue`.
   */
  canWork: boolean;
  canDeleteProject: boolean;
};

const ProjectContext = createContext<ProjectContextValue | undefined>(undefined);

export function ProjectProvider({
  projectId,
  children,
}: {
  projectId: number;
  children: React.ReactNode;
}) {
  const { user } = useSession();
  const { data: project, isLoading: loadingProject } = useProject(projectId);
  const { data: participants, isLoading: loadingParticipants } =
    useProjectParticipants(projectId);

  const value = useMemo<ProjectContextValue>(() => {
    const participantIds = new Set((participants?.content ?? []).map((p) => p.userId));
    const leaderId = project?.leaderId;
    return {
      projectId,
      project,
      isLoading: loadingProject || loadingParticipants,
      participantIds,
      isLeader: !!user && user.id === leaderId,
      isParticipant: !!user && participantIds.has(user.id),
      canManage: editorOrProjectLeader(user, leaderId),
      canWork: editorLeaderOrParticipant(user, leaderId, participantIds),
      canDeleteProject: isEditorOrAbove(user),
    };
  }, [project, participants, user, projectId, loadingProject, loadingParticipants]);

  return <ProjectContext.Provider value={value}>{children}</ProjectContext.Provider>;
}

export function useProjectContext() {
  const ctx = useContext(ProjectContext);
  if (!ctx) throw new Error("useProjectContext must be used within ProjectProvider");
  return ctx;
}
