"use client";

import { useRef } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { useUploadAvatar, useRemoveAvatar } from "@/lib/hooks/useUsers";

// Mirrors the backend's rules (UserAvatarService / docs/API.md "Multipart requests") - the
// server is still the source of truth and re-checks both, this is purely to avoid making a
// doomed request and to give feedback before a multi-second upload even starts.
// WebP is not accepted: the backend now decodes and re-encodes every upload, and the JDK's
// stock ImageIO has no WebP reader.
const MAX_AVATAR_BYTES = 2 * 1024 * 1024;
const ALLOWED_TYPES = ["image/png", "image/jpeg"];

/**
 * Deliberately its own control, not a field inside ProfileEditDialog: that dialog is a
 * react-hook-form + zod JSON form bound to a single mutation, while this uploads a file to a
 * different endpoint with a different error vocabulary (413/415) and out-of-band pending state
 * zod has no way to validate. Keeping them separate keeps both simple.
 */
export function AvatarUploadControl({
  userId,
  hasAvatar,
}: {
  userId: number;
  hasAvatar: boolean;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const uploadAvatar = useUploadAvatar(userId);
  const removeAvatar = useRemoveAvatar(userId);
  const isPending = uploadAvatar.isPending || removeAvatar.isPending;

  const handleFileSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file after a failed attempt
    if (!file) return;

    if (!ALLOWED_TYPES.includes(file.type)) {
      toast.error("Only PNG or JPEG images are allowed.");
      return;
    }
    if (file.size > MAX_AVATAR_BYTES) {
      toast.error("Image must be 2MB or smaller.");
      return;
    }

    uploadAvatar.mutate(file, {
      onSuccess: () => toast.success("Photo updated."),
    });
  };

  return (
    <div className="flex items-center gap-2">
      <input
        ref={inputRef}
        type="file"
        accept="image/png,image/jpeg"
        className="hidden"
        onChange={handleFileSelected}
      />
      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={isPending}
        onClick={() => inputRef.current?.click()}
      >
        Change photo
      </Button>
      {hasAvatar && (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          disabled={isPending}
          onClick={() => removeAvatar.mutate(undefined, { onSuccess: () => toast.success("Photo removed.") })}
        >
          Remove
        </Button>
      )}
    </div>
  );
}
