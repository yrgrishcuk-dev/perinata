import type { AlarmType, BmiCategory, Severity } from "./types";

export function formatDate(value?: string | null): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }
  return date.toLocaleDateString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
  });
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }
  return date.toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

export function todayIso(): string {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

export function bmiCategoryLabel(category?: BmiCategory | null): string {
  switch (category) {
    case "UNDERWEIGHT":
      return "Дефицит массы";
    case "NORMAL":
      return "Норма";
    case "OVERWEIGHT":
      return "Избыточная масса";
    case "OBESE":
      return "Ожирение";
    default:
      return "Не рассчитан";
  }
}

export function gainStatusLabel(status?: string | null): string {
  switch (status) {
    case "BELOW":
      return "Ниже коридора";
    case "WITHIN":
      return "В коридоре";
    case "ABOVE":
      return "Выше коридора";
    default:
      return "Нет данных";
  }
}

export function gainStatusClass(status?: string | null): string {
  switch (status) {
    case "BELOW":
      return "warn";
    case "WITHIN":
      return "ok";
    case "ABOVE":
      return "danger";
    default:
      return "neutral";
  }
}

export function alarmTypeLabel(type: AlarmType): string {
  switch (type) {
    case "SYMPTOM":
      return "Тревожный симптом";
    case "BP":
      return "Давление";
    case "GLUCOSE":
      return "Глюкоза";
    case "CONTRACTION":
      return "Схватки";
    case "WEIGHT":
      return "Прибавка веса";
    case "MISSED_DIARY":
      return "Пропуск дневника";
    case "WATERS":
      return "Отошли воды";
    default:
      return type;
  }
}

export function severityLabel(severity: Severity): string {
  return severity === "CRITICAL" ? "Критично" : severity === "WARNING" ? "Внимание" : "Инфо";
}

const symptomLabels: Record<string, string> = {
  BLEEDING: "кровянистые выделения",
  LIQUID_DISCHARGE: "обильные жидкие выделения",
  HEADACHE_VISION: "головная боль / нарушение зрения",
  SUDDEN_EDEMA: "внезапные отёки",
  FEVER: "температура 38 °C",
  ITCHING: "зуд и жжение",
  URINATION_PAIN: "боль при мочеиспускании",
  ABDOMINAL_PAIN: "сильная боль в животе",
  CONTRACTIONS: "4–5 схваток за час"
};

export function formatPayload(payload: string | null): string {
  if (!payload) {
    return "";
  }
  try {
    const data = JSON.parse(payload) as Record<string, unknown>;
    return Object.entries(data)
      .map(([key, value]) => {
        if (key === "symptoms" && Array.isArray(value)) {
          return `${key}: ${value
            .map((code) => symptomLabels[String(code)] ?? String(code))
            .join("; ")}`;
        }
        return `${key}: ${String(value)}`;
      })
      .join(" · ");
  } catch {
    return payload;
  }
}

export function formatSeconds(value?: number | null): string {
  if (value == null) {
    return "—";
  }
  const minutes = Math.floor(value / 60);
  const seconds = value % 60;
  if (minutes === 0) {
    return `${seconds} с`;
  }
  return `${minutes} мин ${String(seconds).padStart(2, "0")} с`;
}
