import { ApiError, getPatientToken, request, setPatientToken } from "./api";

interface PatientSession {
  token: string;
  bound: boolean;
}

export function telegram() {
  return window.Telegram?.WebApp ?? null;
}

export function initTelegram(): void {
  const app = telegram();
  if (!app) {
    return;
  }
  app.ready();
  app.expand();
  try {
    app.setHeaderColor?.("#f6f7f9");
  } catch {
    // старые клиенты Telegram могут не поддерживать метод
  }
}

export function startParam(): string {
  const app = telegram();
  return app?.initDataUnsafe?.start_param ?? "";
}

export async function ensurePatientToken(): Promise<string> {
  const existing = getPatientToken();
  if (existing) {
    return existing;
  }

  const initData = telegram()?.initData ?? "";
  try {
    const session = await request<PatientSession>("/api/patient/session", {
      method: "POST",
      body: { initData }
    });
    setPatientToken(session.token);
    return session.token;
  } catch (error) {
    if (error instanceof ApiError && error.status >= 500) {
      // ниже попробуем dev-режим
    }
  }

  try {
    const session = await request<PatientSession>("/api/dev/patient-session", {
      method: "POST"
    });
    setPatientToken(session.token);
    return session.token;
  } catch {
    throw new Error(
      "Не удалось открыть приложение. Откройте его из Telegram или включите DEV_MODE."
    );
  }
}
