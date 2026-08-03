"use client";

import { useState } from "react";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { useCreateComment } from "@/lib/hooks/useComments";

export function CommentComposer({ projectId, issueId }: { projectId: number; issueId: number }) {
  const [content, setContent] = useState("");
  const createComment = useCreateComment(projectId, issueId);

  const submit = () => {
    if (!content.trim()) return;
    createComment.mutate({ content }, { onSuccess: () => setContent("") });
  };

  return (
    <div className="mb-4 space-y-2">
      <Textarea
        placeholder="Leave a comment…"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={3}
        onKeyDown={(e) => {
          if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) submit();
        }}
      />
      <div className="flex items-center justify-between">
        <p className="text-xs text-slate">⌘/Ctrl + Enter to send</p>
        <Button size="sm" onClick={submit} disabled={!content.trim() || createComment.isPending}>
          Comment
        </Button>
      </div>
    </div>
  );
}
