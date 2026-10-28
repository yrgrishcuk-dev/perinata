import { ApiError, getPatientToken, request, setPatientToken } from "./api";

interface PatientSession {
  token: string;
  bound: boolean;
}

export function telegram() {
  return window.Telegram?.WebApp ?? null;
}

export function maxApp() {
  return window.WebApp ?? null;
}

export function initTelegram(): void {
  const app = telegram();
  if (app) {
    app.ready();
    app.expand();
    try {
      app.setHeaderColor?.("#f6f7f9");
    } catch {
      // старые клиенты Telegram могут не поддерживать метод
    }
  }
  try {
    maxApp()?.BackButton?.hide();
  } catch {
    // MAX Bridge может быть недоступен вне клиента
  }
}

export function startParam(): string {
  const maxParam = maxApp()?.initDataUnsafe?.start_param;
  if (maxParam) {
    return maxParam;
  }
  const telegramParam = telegram()?.initDataUnsafe?.start_param;
  return telegramParam ?? "";
}

async function openSession(path: string, initData: string): Promise<string> {
  const session = await request<PatientSession>(path, {
    method: "POST",
    body: { initData }
  });
  setPatientToken(session.token);
  return session.token;
}

export async function ensurePatientToken(): Promise<string> {
  const existing = getPatientToken();
  if (existing) {
    return existing;
  }

  const maxInitData = maxApp()?.initData ?? "";
  if (maxInitData) {
    return openSession("/api/patient/session/max", maxInitData);
  }

  const telegramInitData = telegram()?.initData ?? "";
  if (telegramInitData) {
    return openSession("/api/patient/session", telegramInitData);
  }

  try {
    return await openSession("/api/patient/session", "");
  } catch (error) {
    if (!(error instanceof ApiError)) {
      throw error;
    }
  }

  try {
    return await openSession("/api/dev/patient-session", "");
  } catch {
    throw new Error(
      "Не удалось открыть приложение. Откройте его из Telegram или MAX."
    );
  }
}
