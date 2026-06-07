import type { ReactNode } from "react";

import { Label } from "@/components/ui/label";

interface FormFieldProps {
  id: string;
  label: string;
  error?: string;
  description?: string;
  children: ReactNode;
}

export const FormField = ({
  id,
  label,
  error,
  description,
  children,
}: FormFieldProps) => (
  <div className="space-y-2">
    <Label htmlFor={id}>{label}</Label>
    {children}
    {error ? (
      <p className="text-sm font-medium text-red-500">{error}</p>
    ) : description ? (
      <p className="text-sm text-muted-foreground">{description}</p>
    ) : null}
  </div>
);
