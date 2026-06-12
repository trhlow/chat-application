import type { FormEvent } from "react";

import { Button } from "@/components/ui/button";

interface SendFriendRequestFormProps {
  disabled?: boolean;
  onSubmit: () => void;
}

export const SendFriendRequestForm = ({
  disabled,
  onSubmit,
}: SendFriendRequestFormProps) => {
  const submit = (event: FormEvent) => {
    event.preventDefault();
    onSubmit();
  };

  return (
    <form onSubmit={submit}>
      <Button type="submit" size="sm" disabled={disabled}>
        Gửi lời mời
      </Button>
    </form>
  );
};
