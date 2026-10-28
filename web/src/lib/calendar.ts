export type EddTone = "today" | "soon" | "upcoming" | "later" | "past";

const DAY_MS = 24 * 60 * 60 * 1000;

export function parseIsoDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) {
    return null;
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(year, month - 1, day, 12, 0, 0, 0);
  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day
  ) {
    return null;
  }
  return date;
}

export function toIsoDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days, 12, 0, 0, 0);
}

export function startOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1, 12, 0, 0, 0);
}

export function endOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0, 12, 0, 0, 0);
}

export function startOfWeekMonday(date: Date): Date {
  const weekday = date.getDay();
  const shift = weekday === 0 ? -6 : 1 - weekday;
  return addDays(date, shift);
}

export function buildMonthGrid(month: Date): Date[] {
  const start = startOfWeekMonday(startOfMonth(month));
  return Array.from({ length: 42 }, (_, index) => addDays(start, index));
}

export function daysBetween(from: Date, to: Date): number {
  const fromUtc = Date.UTC(from.getFullYear(), from.getMonth(), from.getDate());
  const toUtc = Date.UTC(to.getFullYear(), to.getMonth(), to.getDate());
  return Math.round((toUtc - fromUtc) / DAY_MS);
}

export function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export function isWeekend(date: Date): boolean {
  const weekday = date.getDay();
  return weekday === 0 || weekday === 6;
}

export function eddTone(edd: Date, today: Date): EddTone {
  const days = daysBetween(today, edd);
  if (days < 0) {
    return "past";
  }
  if (days === 0) {
    return "today";
  }
  if (days <= 7) {
    return "soon";
  }
  if (days <= 14) {
    return "upcoming";
  }
  return "later";
}

export function daysLabel(days: number): string {
  if (days === 0) {
    return "сегодня";
  }
  if (days === 1) {
    return "завтра";
  }
  if (days === -1) {
    return "вчера";
  }
  if (days < 0) {
    return `ПДР прошла ${Math.abs(days)} дн. назад`;
  }
  return `через ${days} дн.`;
}

export function shortName(fullName: string): string {
  const parts = fullName.trim().split(/\s+/);
  if (parts.length >= 2 && parts[1].length > 0) {
    return `${parts[0]} ${parts[1][0]}.`;
  }
  return parts[0] ?? fullName;
}

export function formatMonthTitle(date: Date): string {
  const value = date.toLocaleDateString("ru-RU", { month: "long", year: "numeric" });
  return value.charAt(0).toUpperCase() + value.slice(1);
}

export function formatDayTitle(date: Date): string {
  const value = date.toLocaleDateString("ru-RU", {
    weekday: "long",
    day: "numeric",
    month: "long"
  });
  return value.charAt(0).toUpperCase() + value.slice(1);
}

export function isSameMonth(date: Date, month: Date): boolean {
  return date.getFullYear() === month.getFullYear() && date.getMonth() === month.getMonth();
}
