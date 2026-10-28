import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode
} from "react";
import {
  ApiError,
  clearPatientToken,
  clearStaffToken,
  getPatientToken,
  getStaffToken,
  request,
  setPatientToken,
  setStaffToken
} from "./api";
import { ensurePatientToken } from "./telegram";
import type { LoginResponse, PatientMe, StaffMe } from "./types";

interface PatientState {
  token: string | null;
  me: PatientMe | null;
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
}

const PatientContext = createContext<PatientState | null>(null);

export function PatientProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(getPatientToken());
  const [me, setMe] = useState<PatientMe | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      let current = getPatientToken();
      if (!current) {
        current = await ensurePatientToken();
      }
      let data: PatientMe;
      try {
        data = await request<PatientMe>("/api/patient/me", { token: current });
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          clearPatientToken();
          current = await ensurePatientToken();
          data = await request<PatientMe>("/api/patient/me", { token: current });
        } else {
          throw error;
        }
      }
      setToken(current);
      setMe(data);
    } catch (error) {
      setError(error instanceof Error ? error.message : "Ошибка загрузки");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const value = useMemo(
    () => ({ token, me, loading, error, refresh }),
    [token, me, loading, error, refresh]
  );

  return <PatientContext.Provider value={value}>{children}</PatientContext.Provider>;
}

export function usePatient(): PatientState {
  const context = useContext(PatientContext);
  if (!context) {
    throw new Error("PatientProvider отсутствует");
  }
  return context;
}

interface StaffState {
  token: string | null;
  me: StaffMe | null;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<LoginResponse>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const StaffContext = createContext<StaffState | null>(null);

export function StaffProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(getStaffToken());
  const [me, setMe] = useState<StaffMe | null>(null);
  const [loading, setLoading] = useState<boolean>(Boolean(getStaffToken()));
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    const current = getStaffToken();
    if (!current) {
      setMe(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await request<StaffMe>("/api/auth/me", { token: current });
      setMe(data);
      setError(null);
    } catch (error) {
      clearStaffToken();
      setToken(null);
      setMe(null);
      setError(error instanceof Error ? error.message : "Требуется вход");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const login = useCallback(async (email: string, password: string) => {
    const response = await request<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: { email, password }
    });
    setStaffToken(response.token);
    setToken(response.token);
    const profile = await request<StaffMe>("/api/auth/me", { token: response.token });
    setMe(profile);
    return response;
  }, []);

  const logout = useCallback(async () => {
    const current = getStaffToken();
    if (current) {
      try {
        await request<void>("/api/auth/logout", { method: "POST", token: current });
      } catch {
        // выходим локально в любом случае
      }
    }
    clearStaffToken();
    setToken(null);
    setMe(null);
  }, []);

  const value = useMemo(
    () => ({ token, me, loading, error, login, logout, refresh }),
    [token, me, loading, error, login, logout, refresh]
  );

  return <StaffContext.Provider value={value}>{children}</StaffContext.Provider>;
}

export function useStaff(): StaffState {
  const context = useContext(StaffContext);
  if (!context) {
    throw new Error("StaffProvider отсутствует");
  }
  return context;
}

export function savePatientSession(token: string): void {
  setPatientToken(token);
}
