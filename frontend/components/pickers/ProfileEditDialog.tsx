"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useUpdateProfile } from "@/lib/hooks/useUsers";
import type { UserResponse } from "@/lib/api/types";

const schema = z.object({
  name: z.string().min(2, "At least 2 characters").max(255),
  surname: z.string().min(2, "At least 2 characters").max(255),
  email: z.string().min(1, "Email is required").email("Enter a valid email"),
});
type FormValues = z.infer<typeof schema>;

export function ProfileEditDialog({
  open,
  onOpenChange,
  user,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  user: UserResponse;
}) {
  const updateProfile = useUpdateProfile(user.id);
  const { register, handleSubmit, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    values: { name: user.name, surname: user.surname, email: user.email },
  });

  const onSubmit = (values: FormValues) => {
    updateProfile.mutate(values, {
      onSuccess: () => { toast.success("Profile updated."); onOpenChange(false); },
    });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-sm">
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogHeader>
            <DialogTitle>Edit profile</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 py-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="name">First name</Label>
                <Input id="name" {...register("name")} />
                {errors.name && <p className="text-xs text-rust">{errors.name.message}</p>}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="surname">Last name</Label>
                <Input id="surname" {...register("surname")} />
                {errors.surname && <p className="text-xs text-rust">{errors.surname.message}</p>}
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" {...register("email")} />
              {errors.email && <p className="text-xs text-rust">{errors.email.message}</p>}
            </div>
          </div>
          <DialogFooter>
            <Button type="submit" disabled={updateProfile.isPending}>Save changes</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
