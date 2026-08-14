import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { cn } from "@/lib/utils";

type UserAvatarProps = {
  name: string;
  surname: string;
  avatarUrl?: string | null;
  className?: string;
  /** Overrides the fallback initials' text size - the two current call sites want different ones. */
  fallbackClassName?: string;
};

/**
 * Single home for the initials logic that used to be duplicated in AppRail and
 * UserProfileClient. Radix's AvatarImage renders AvatarFallback whenever `src` is undefined
 * *or* the image fails to load - which is exactly what happens once a presigned avatarUrl
 * expires, so an avatar quietly degrades back to initials instead of showing a broken image.
 */
export function UserAvatar({
  name,
  surname,
  avatarUrl,
  className,
  fallbackClassName,
}: UserAvatarProps) {
  const initials = `${name[0]}${surname[0]}`.toUpperCase();

  return (
    <Avatar className={cn("h-7 w-7", className)}>
      {avatarUrl && <AvatarImage src={avatarUrl} alt={`${name} ${surname}`} />}
      <AvatarFallback className={cn("bg-secondary text-xs font-medium", fallbackClassName)}>
        {initials}
      </AvatarFallback>
    </Avatar>
  );
}
