import type { ChangeEvent } from "react";

import { Camera } from "lucide-react";

interface AvatarUploaderProps {
  disabled?: boolean;
  onSelect: (file: File) => void;
}

export const AvatarUploader = ({ disabled, onSelect }: AvatarUploaderProps) => {
  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) onSelect(file);
    event.target.value = "";
  };

  return (
    <label className="inline-flex h-9 cursor-pointer items-center rounded-lg border border-border px-3 text-xs font-semibold hover:bg-muted">
      <Camera className="mr-2 h-4 w-4" />
      Avatar
      <input
        type="file"
        accept="image/*"
        className="sr-only"
        disabled={disabled}
        onChange={handleChange}
      />
    </label>
  );
};
