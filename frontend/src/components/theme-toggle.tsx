import { MoonStar, SunMedium } from "lucide-react";

import { Button } from "@/components/ui/button";

import { useTheme } from "./theme-provider";

export const ThemeToggle = () => {
  const { theme, toggleTheme } = useTheme();

  return (
    <Button
      variant="outline"
      size="icon"
      className="rounded-full bg-background/70 backdrop-blur"
      onClick={toggleTheme}
      aria-label="Toggle theme"
    >
      {theme === "dark" ? (
        <SunMedium className="h-4 w-4" />
      ) : (
        <MoonStar className="h-4 w-4" />
      )}
    </Button>
  );
};
