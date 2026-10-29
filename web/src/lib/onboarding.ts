export type OnboardingStep = "patient" | "qr" | "messenger" | "calendar" | "help";

export interface OnboardingStepInfo {
  key: OnboardingStep;
  title: string;
  description: string;
  to: string;
  auto?: boolean;
}

export const ONBOARDING_STEPS: OnboardingStepInfo[] = [
  {
    key: "patient",
    title: "Создайте первую карточку пациентки",
    description: "ФИО, возраст, ПДР, рост и вес до беременности.",
    to: "/cabinet/patients",
    auto: true
  },
  {
    key: "qr",
    title: "Покажите пациентке QR",
    description: "Одноразовая ссылка привяжет её Telegram или MAX к карточке.",
    to: "/cabinet/patients"
  },
  {
    key: "messenger",
    title: "Подключите уведомления",
    description: "Кнопки Telegram и MAX в шапке кабинета. Один активный канал.",
    to: "/cabinet/help#notifications"
  },
  {
    key: "calendar",
    title: "Откройте календарь",
    description: "Планируйте выходные и праздники вокруг предполагаемых дат родов.",
    to: "/cabinet/calendar",
    auto: true
  },
  {
    key: "help",
    title: "Прочитайте инструкцию",
    description: "Короткий разбор всех разделов кабинета.",
    to: "/cabinet/help",
    auto: true
  }
];

export interface OnboardingState {
  steps: Record<OnboardingStep, boolean>;
  dismissed: boolean;
}

function emptySteps(): Record<OnboardingStep, boolean> {
  return {
    patient: false,
    qr: false,
    messenger: false,
    calendar: false,
    help: false
  };
}

function storageKey(doctorId: number): string {
  return `perinata.onboarding.${doctorId}`;
}

export function loadOnboarding(doctorId: number): OnboardingState {
  try {
    const raw = localStorage.getItem(storageKey(doctorId));
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<OnboardingState> | null;
      return {
        steps: { ...emptySteps(), ...(parsed?.steps ?? {}) },
        dismissed: Boolean(parsed?.dismissed)
      };
    }
  } catch {
    // localStorage может быть недоступен
  }
  return { steps: emptySteps(), dismissed: false };
}

export function saveOnboarding(doctorId: number, state: OnboardingState): void {
  try {
    localStorage.setItem(storageKey(doctorId), JSON.stringify(state));
  } catch {
    // ignore
  }
}

export function markOnboardingStep(doctorId: number, step: OnboardingStep): OnboardingState {
  const state = loadOnboarding(doctorId);
  state.steps[step] = true;
  saveOnboarding(doctorId, state);
  return state;
}

export function setOnboardingDismissed(doctorId: number, dismissed: boolean): OnboardingState {
  const state = loadOnboarding(doctorId);
  state.dismissed = dismissed;
  saveOnboarding(doctorId, state);
  return state;
}

export function completedSteps(state: OnboardingState): number {
  return ONBOARDING_STEPS.filter((step) => state.steps[step.key]).length;
}
