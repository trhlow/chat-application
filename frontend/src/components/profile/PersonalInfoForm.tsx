import type { ComponentPropsWithoutRef } from "react";

export const PersonalInfoForm = (props: ComponentPropsWithoutRef<"form">) => (
  <form {...props} className={`space-y-4 ${props.className ?? ""}`} />
);
