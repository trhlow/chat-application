export function formatIsoDate(value: string | Date, locale = "en-US"): string {
  const date = typeof value === "string" ? new Date(value) : value;
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

export function toIsoString(value: Date): string {
  return value.toISOString();
}
