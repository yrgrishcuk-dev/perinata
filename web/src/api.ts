const PATIENT_TOKEN_KEY = "antenatal.patient.token";
const STAFF_TOKEN_KEY = "antenatal.staff.token";

let patientTokenMemory: string | null = null;
let staffTokenMemory: string | null = null;

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  token?: string | null;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (options.token) {
    headers["Authorization"] = `Bearer ${options.token}`;
  }

  const response = await fetch(path, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body)
  });

  const text = await response.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const message =
      data && typeof data === "object" && "error" in data
        ? String((data as { error: unknown }).error)
        : `Ошибка ${response.status}`;
    throw new ApiError(response.status, message);
  }

  return data as T;
}

export function getPatientToken(): string | null {
  try {
    const stored = sessionStorage.getItem(PATIENT_TOKEN_KEY);
    if (stored) {
      patientTokenMemory = stored;
    }
  } catch {
    // В некоторых webview стороннее хранилище запрещено — работаем в памяти.
  }
  return patientTokenMemory;
}

export function setPatientToken(token: string): void {
  patientTokenMemory = token;
  try {
    sessionStorage.setItem(PATIENT_TOKEN_KEY, token);
  } catch {
    // ignore
  }
}

export function clearPatientToken(): void {
  patientTokenMemory = null;
  try {
    sessionStorage.removeItem(PATIENT_TOKEN_KEY);
  } catch {
    // ignore
  }
}

export function getStaffToken(): string | null {
  try {
    const stored = sessionStorage.getItem(STAFF_TOKEN_KEY);
    if (stored) {
      staffTokenMemory = stored;
    }
  } catch {
    // ignore
  }
  return staffTokenMemory;
}

export function setStaffToken(token: string): void {
  staffTokenMemory = token;
  try {
    sessionStorage.setItem(STAFF_TOKEN_KEY, token);
  } catch {
    // ignore
  }
}

export function clearStaffToken(): void {
  staffTokenMemory = null;
  try {
    sessionStorage.removeItem(STAFF_TOKEN_KEY);
  } catch {
    // ignore
  }
}
