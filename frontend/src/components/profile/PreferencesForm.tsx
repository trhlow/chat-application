import type { ComponentPropsWithoutRef } from "react";

export const PreferencesForm = (props: ComponentPropsWithoutRef<"form">) => (
  <form {...props} className={`space-y-4 ${props.className ?? ""}`} />
);
