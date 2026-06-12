import { LogOut } from "lucide-react";

import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/useAuthStore";

export const Logout = () => {
  const signout = useAuthStore((state) => state.signout);

  return (
    <Button type="button" variant="ghost" onClick={() => void signout()}>
      <LogOut className="h-4 w-4" />
      Đăng xuất
    </Button>
  );
};
