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
  LegEdema,
  PatientResponse,
  Severity
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

export function dischargeCharacterLabel(value: DischargeCharacter): string {
  switch (value) {
    case "NONE":
      return "���";
    case "NORMAL":
      return "�������";
    case "MUCOUS":
      return "���������";
    case "WATERY":
      return "����������";
    case "BLOODY":
      return "�����������";
    default:
      return value;
  }
}

export function dischargeAmountLabel(value: DischargeAmount): string {
  return value === "ABUNDANT" ? "�������" : "������";
}

export function fetalMovementsLabel(value: FetalMovements): string {
  switch (value) {
    case "NORMAL":
      return "�������";
    case "LESS":
      return "������ ��������";
    case "NONE":
      return "���";
    default:
      return "�� �����������";
  }
}

export function abdominalPainLabel(value: AbdominalPain): string {
  switch (value) {
    case "NONE":
      return "���";
    case "PULLING":
      return "�������";
    case "CRAMPING":
      return "���������������";
    case "SHARP":
      return "������";
    default:
      return "������";
  }
}

export function legEdemaLabel(value: LegEdema): string {
  switch (value) {
    case "NONE":
      return "���";
    case "MILD":
      return "���������";
    case "PRONOUNCED":
      return "����������";
    default:
      return "�������������";
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
      discharge += ", �����";
    }
    parts.push(`���������: ${discharge}`);
  }
  if (entry.vaginalPh != null) {
    parts.push(`pH: ${entry.vaginalPh}`);
  }
  if (entry.fetalMovements) {
    parts.push(`���������: ${fetalMovementsLabel(entry.fetalMovements).toLowerCase()}`);
  }
  if (entry.lowerAbdominalPain) {
    const pain = abdominalPainLabel(entry.lowerAbdominalPain);
    const intensity = entry.painIntensity != null ? ` ${entry.painIntensity}/10` : "";
    parts.push(`����: ${pain}${intensity}`);
  }
  if (entry.legEdema) {
    parts.push(`����: ${legEdemaLabel(entry.legEdema).toLowerCase()}`);
  }
  return parts.join(" � ");
}
