"use client";

import { useState } from "react";
import { SpineRow } from "@/components/spine/SpineRow";
import { UserName } from "@/lib/users/directory";
import { useSession } from "@/lib/auth/session";
import { useProjectContext } from "@/lib/project/ProjectContext";
import { canEditComment, canDeleteComment } from "@/lib/auth/can";
import { useUpdateComment, useDeleteComment } from "@/lib/hooks/useComments";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import type { CommentResponse } from "@/lib/api/types";

export function CommentRow({
  comment,
  projectId,
  issueId,
  showDate,
}: {
  comment: CommentResponse;
  projectId: number;
  issueId: number;
  showDate?: boolean;
}) {
  const { user } = useSession();
  const { project } = useProjectContext();
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(comment.content);
  const updateComment = useUpdateComment(projectId, issueId);
  const deleteComment = useDeleteComment(projectId, issueId);

  const editable = canEditComment(user, comment.userId);
  const deletable = canDeleteComment(user, comment.userId, project?.leaderId);
  const edited = comment.updatedAt > comment.createdAt;

  return (
    <SpineRow timestamp={comment.createdAt} showDate={showDate} dotClassName="bg-ink">
      <div className="rounded border border-rule bg-secondary/40 p-2.5">
        <div className="mb-1 flex items-center justify-between">
          <p className="text-sm font-medium">
            <UserName id={comment.userId} />
            {edited && <span className="ml-1.5 text-xs font-normal text-slate">(edited)</span>}
          </p>
          {(editable || deletable) && !editing && (
            <div className="flex gap-2 opacity-0 transition-opacity group-hover:opacity-100">
              {editable && (
                <button
                  onClick={() => setEditing(true)}
                  className="text-xs text-slate hover:text-signal"
                >
                  Edit
                </button>
              )}
              {deletable && (
                <AlertDialog>
                  <AlertDialogTrigger asChild>
                    <button className="text-xs text-slate hover:text-rust">Delete</button>
                  </AlertDialogTrigger>
                  <AlertDialogContent>
                    <AlertDialogHeader>
                      <AlertDialogTitle>Delete this comment?</AlertDialogTitle>
                      <AlertDialogDescription>This can't be undone.</AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                      <AlertDialogCancel>Cancel</AlertDialogCancel>
                      <AlertDialogAction onClick={() => deleteComment.mutate(comment.id)}>
                        Delete
                      </AlertDialogAction>
                    </AlertDialogFooter>
                  </AlertDialogContent>
                </AlertDialog>
              )}
            </div>
          )}
        </div>

        {editing ? (
          <div className="space-y-2">
            <Textarea value={content} onChange={(e) => setContent(e.target.value)} rows={3} />
            <div className="flex gap-2">
              <Button
                size="sm"
                onClick={() =>
                  updateComment.mutate(
                    { commentId: comment.id, body: { content } },
                    { onSuccess: () => setEditing(false) },
                  )
                }
                disabled={updateComment.isPending}
              >
                Save
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setEditing(false)}>
                Cancel
              </Button>
            </div>
          </div>
        ) : (
          <p className="whitespace-pre-wrap text-sm">{comment.content}</p>
        )}
      </div>
    </SpineRow>
  );
}
