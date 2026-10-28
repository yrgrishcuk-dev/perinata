import { useCallback, useEffect, useState } from "react";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDateTime, formatSeconds } from "../format";
import { usePatient } from "../session";
import type { ContractionState } from "../types";

export default function ContractionsPage() {
  const { token, refresh } = usePatient();
  const [state, setState] = useState<ContractionState | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeStart, setActiveStart] = useState<string | null>(null);
  const [now, setNow] = useState(Date.now());
  const [watersMessage, setWatersMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    setLoading(true);
    try {
      const data = await request<ContractionState>("/api/patient/contractions", { token });
      setState(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить счётчик");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!activeStart) {
      return;
    }
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [activeStart]);

  if (loading) {
    return <Spinner />;
  }
  if (error && !state) {
    return <ErrorBox message={error} onRetry={() => void load()} />;
  }

  const start = () => {
    setActiveStart(new Date().toISOString());
    setNow(Date.now());
    window.Telegram?.WebApp?.HapticFeedback?.impactOccurred("medium");
  };

  const stop = async () => {
    if (!token || !activeStart) {
      return;
    }
    setBusy(true);
    try {
      const data = await request<ContractionState>("/api/patient/contractions", {
        method: "POST",
        body: { startAt: activeStart, endAt: new Date().toISOString() },
        token
      });
      setState(data);
      setActiveStart(null);
      setError(null);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось сохранить схватку");
    } finally {
      setBusy(false);
    }
  };

  const waters = async () => {
    if (!token || !window.confirm("Подтвердите: отошли воды?")) {
      return;
    }
    setBusy(true);
    try {
      const data = await request<{ critical: boolean; message: string }>(
        "/api/patient/contractions/waters",
        { method: "POST", token }
      );
      setWatersMessage(data.message);
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось отправить сообщение");
    } finally {
      setBusy(false);
    }
  };

  const closeSession = async () => {
    if (!token) {
      return;
    }
    try {
      await request<void>("/api/patient/contractions/close", { method: "POST", token });
      setActiveStart(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось завершить сеанс");
    }
  };

  if (watersMessage) {
    return (
      <div className="redscreen overlay">
        <div className="red-top">
          <h1>Отошли воды</h1>
          <p>{watersMessage}</p>
        </div>
        <a className="btn btn-danger btn-block btn-lg" href="tel:103">
          Позвонить 103
        </a>
        <a className="btn btn-block mt" href="tel:112">
          Позвонить 112
        </a>
        <button className="btn btn-ghost btn-block mt" onClick={() => setWatersMessage(null)}>
          Вернуться
        </button>
      </div>
    );
  }

  const elapsed = activeStart ? Math.max(0, Math.floor((now - new Date(activeStart).getTime()) / 1000)) : 0;
  const severityClass =
    state?.severity === "CRITICAL" ? "danger" : state?.severity === "WARNING" ? "warn" : "info";

  return (
    <>
      <div className="card">
        <h1 style={{ fontSize: 21, marginBottom: 6 }}>Счётчик схваток</h1>
        <p className="muted" style={{ marginBottom: 14 }}>
          Нажмите кнопку в начале схватки и ещё раз — когда она закончилась. Приложение посчитает
          частоту и подскажет, когда пора в роддом.
        </p>

        <button
          className={`btn btn-block btn-lg ${activeStart ? "btn-danger" : "btn-primary"}`}
          disabled={busy}
          onClick={() => (activeStart ? void stop() : start())}
        >
          {activeStart ? `Закончилась · ${formatSeconds(elapsed)}` : "Началась схватка"}
        </button>

        {activeStart ? (
          <p className="muted center mt">Идёт схватка. Нажмите кнопку, когда закончится.</p>
        ) : null}

        {state && state.sessionId ? (
          <button className="btn btn-ghost btn-block mt" onClick={() => void closeSession()}>
            Завершить сеанс наблюдения
          </button>
        ) : null}
      </div>

      {state ? (
        <div className={`alert ${severityClass}`}>
          <b>{state.message}</b>
          <div className="muted" style={{ marginTop: 6 }}>
            За последний час: {state.countLastHour} схваток
            {state.averageIntervalSeconds != null
              ? ` · интервал ${formatSeconds(state.averageIntervalSeconds)}`
              : ""}
            {state.averageDurationSeconds != null
              ? ` · длительность ${formatSeconds(state.averageDurationSeconds)}`
              : ""}
          </div>
        </div>
      ) : null}

      <div className="card">
        <h2>Схватки</h2>
        {!state || state.contractions.length === 0 ? (
          <p className="muted mt">Пока нет записей. Начните с кнопки выше.</p>
        ) : (
          <div className="list mt">
            {state.contractions
              .slice()
              .reverse()
              .map((contraction) => (
                <div className="list-item" key={contraction.id}>
                  <div className="row between">
                    <b>{formatDateTime(contraction.startAt)}</b>
                    <span className="badge neutral">{formatSeconds(contraction.durationSeconds)}</span>
                  </div>
                </div>
              ))}
          </div>
        )}
      </div>

      <div className="card">
        <h2>Отошли воды?</h2>
        <p className="muted" style={{ marginBottom: 12 }}>
          Если воды отошли, не ждите схваток — поезжайте в родильный дом или вызовите 103.
        </p>
        <button className="btn btn-soft-danger btn-block" disabled={busy} onClick={() => void waters()}>
          Отошли воды
        </button>
      </div>

      {error ? <ErrorBox message={error} /> : null}
    </>
  );
}
