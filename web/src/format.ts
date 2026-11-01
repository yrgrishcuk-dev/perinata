import type {
  AbdominalPain,
  AlarmType,
  AppointmentStatus,
  AppointmentType,
  BmiCategory,
  DiaryView,
  DischargeAmount,
  DischargeCharacter,
  FetalMovements,
  Headache,
  LegEdema,
  PatientResponse,
  Severity,
  SleepQuality
} from "./types";

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

export function appointmentTypeLabel(type: AppointmentType): string {
  switch (type) {
    case "VISIT":
      return "Приём";
    case "ULTRASOUND":
      return "УЗИ";
    case "TESTS":
      return "Анализы";
    case "OTHER":
      return "Другое";
    default:
      return type;
  }
}

export function appointmentStatusLabel(status: AppointmentStatus): string {
  switch (status) {
    case "COMPLETED":
      return "Состоялся";
    case "NO_SHOW":
      return "Не пришёл";
    case "CANCELLED":
      return "Отменён";
    default:
      return "Запланирован";
  }
}

export function appointmentStatusClass(status: AppointmentStatus): string {
  switch (status) {
    case "COMPLETED":
      return "ok";
    case "NO_SHOW":
      return "danger";
    case "CANCELLED":
      return "neutral";
    default:
      return "neutral";
  }
}

export function patientResponseLabel(response: PatientResponse): string {
  switch (response) {
    case "CONFIRMED":
      return "Подтверждён";
    case "DECLINED":
      return "Не сможет прийти";
    default:
      return "Ожидает подтверждения";
  }
}

export function patientResponseClass(response: PatientResponse): string {
  switch (response) {
    case "CONFIRMED":
      return "ok";
    case "DECLINED":
      return "danger";
    default:
      return "neutral";
  }
}

export function appointmentWhen(date: string, time: string): string {
  return `${formatDate(date)} в ${time.slice(0, 5)}`;
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
    case "APPOINTMENT_RESPONSE":
      return "Ответ на приём";
    case "DISCHARGE":
      return "Выделения";
    case "VAGINAL_PH":
      return "pH влагалища";
    case "FETAL_MOVEMENTS":
      return "Шевеления плода";
    case "ABDOMINAL_PAIN":
      return "Боль внизу живота";
    case "EDEMA":
      return "Отёки ног";
    case "HEADACHE":
      return "Головная боль";
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

export function formatKickDuration(value?: number | null): string {
  if (value == null || value < 0) {
    return "00:00";
  }
  const minutes = Math.floor(value / 60);
  const seconds = value % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

export function dischargeCharacterLabel(value: DischargeCharacter): string {
  switch (value) {
    case "NONE":
      return "Нет";
    case "NORMAL":
      return "Обычные";
    case "MUCOUS":
      return "Слизистые";
    case "WATERY":
      return "Водянистые";
    case "BLOODY":
      return "Кровянистые";
    default:
      return value;
  }
}

export function dischargeAmountLabel(value: DischargeAmount): string {
  return value === "ABUNDANT" ? "Обильно" : "Обычно";
}

export function fetalMovementsLabel(value: FetalMovements): string {
  switch (value) {
    case "NORMAL":
      return "Обычные";
    case "LESS":
      return "Меньше обычного";
    case "NONE":
      return "Нет";
    default:
      return "Не отслеживала";
  }
}

export function abdominalPainLabel(value: AbdominalPain): string {
  switch (value) {
    case "NONE":
      return "Нет";
    case "PULLING":
      return "Тянущие";
    case "CRAMPING":
      return "Схваткообразные";
    case "SHARP":
      return "Острые";
    default:
      return "Другое";
  }
}

export function legEdemaLabel(value: LegEdema): string {
  switch (value) {
    case "NONE":
      return "Нет";
    case "MILD":
      return "Небольшие";
    case "PRONOUNCED":
      return "Выраженные";
    default:
      return "Асимметричные";
  }
}

export function sleepQualityLabel(value: SleepQuality): string {
  switch (value) {
    case "GOOD":
      return "Хороший";
    case "RESTLESS":
      return "Беспокойный";
    case "POOR":
      return "Плохой";
    default:
      return "Обычный";
  }
}

export function headacheLabel(value: Headache): string {
  switch (value) {
    case "NONE":
      return "Нет";
    case "MILD":
      return "Лёгкая";
    case "SEVERE":
      return "Сильная";
    default:
      return "Умеренная";
  }
}

export function diaryObservationsSummary(entry: DiaryView): string {
  const parts: string[] = [];
  if (entry.dischargeCharacter) {
    let discharge = dischargeCharacterLabel(entry.dischargeCharacter);
    if (entry.dischargeAmount) {
      discharge += `, ${dischargeAmountLabel(entry.dischargeAmount).toLowerCase()}`;
    }
    if (entry.dischargeOdor) {
      discharge += ", запах";
    }
    parts.push(`Выделения: ${discharge}`);
  }
  if (entry.vaginalPh != null) {
    parts.push(`pH: ${entry.vaginalPh}`);
  }
  if (entry.kickCount > 0) {
    parts.push(
      `Шевеления: ${entry.kickCount} за ${formatKickDuration(entry.kickDurationSeconds)}`
    );
  } else if (entry.fetalMovements) {
    parts.push(`Шевеления: ${fetalMovementsLabel(entry.fetalMovements).toLowerCase()}`);
  }
  if (entry.lowerAbdominalPain) {
    const pain = abdominalPainLabel(entry.lowerAbdominalPain);
    const intensity = entry.painIntensity != null ? ` ${entry.painIntensity}/10` : "";
    parts.push(`Боль: ${pain}${intensity}`);
  }
  if (entry.legEdema) {
    parts.push(`Отёки: ${legEdemaLabel(entry.legEdema).toLowerCase()}`);
  }
  if (entry.sleepQuality) {
    parts.push(`Сон: ${sleepQualityLabel(entry.sleepQuality).toLowerCase()}`);
  }
  if (entry.headache && entry.headache !== "NONE") {
    parts.push(`Головная боль: ${headacheLabel(entry.headache).toLowerCase()}`);
  }
  return parts.join(" · ");
}
