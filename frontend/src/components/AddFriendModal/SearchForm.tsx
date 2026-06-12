import type { FormEvent } from "react";

import { Search } from "lucide-react";

interface SearchFormProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit?: () => void;
}

export const SearchForm = ({ value, onChange, onSubmit }: SearchFormProps) => {
  const submit = (event: FormEvent) => {
    event.preventDefault();
    onSubmit?.();
  };

  return (
    <form className="relative" onSubmit={submit}>
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-10 w-full rounded-lg border border-input bg-background pl-9 pr-3 text-sm outline-none focus:ring-2 focus:ring-ring/20"
        placeholder="Tìm người dùng"
      />
    </form>
  );
};
