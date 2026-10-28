import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { AlarmStatusBadge, ErrorBox, SeverityBadge, Spinner } from "../components/ui";
import { request } from "../api";
import { alarmTypeLabel, formatDateTime, formatPayload } from "../format";
import { useStaff } from "../session";
import type { AlertView, AlarmStatus } from "../types";

const filters: [AlarmStatus | "", string][] = [
  ["NEW", "Новые"],
  ["ACKED", "В работе"],
  ["CLOSED", "Закрытые"],
  ["", "Все"]
];

export default function AlertsPage() {
  const { token } = useStaff();
  const [status, setStatus] = useState<AlarmStatus | "">("NEW");
  const [alerts, setAlerts] = useState<AlertView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    setLoading(true);
    try {
      const suffix = status ? `?status=${status}` : "";
      const data = await request<AlertView[]>(`/api/doctor/alerts${suffix}`, { token });
      setAlerts(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить события");
    } finally {
      setLoading(false);
    }
  }, [token, status]);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return <Spinner />;
  }
  if (error) {
    return <ErrorBox message={error} onRetry={() => void load()} />;
  }

  const act = async (alertId: number, close: boolean) => {
    if (!token) {
      return;
    }
    const comment = window.prompt("Комментарий (необязательно)");
    if (comment === null) {
      return;
    }
    try {
      await request<AlertView>(`/api/doctor/alerts/${alertId}/${close ? "close" : "ack"}`, {
        method: "POST",
        body: { comment },
        token
      });
      setNotice(close ? "Событие закрыто" : "Событие взято в работу");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось обновить событие");
    }
  };

  return (
    <>
      <div className="row between mb">
        <h1 style={{ fontSize: 24 }}>События</h1>
        <button className="btn" onClick={() => void load()}>
          Обновить
        </button>
      </div>

      <div className="tabs">
        {filters.map(([value, label]) => (
          <button
            key={label}
            className={status === value ? "active" : ""}
            onClick={() => setStatus(value)}
            type="button"
          >
            {label}
          </button>
        ))}
      </div>

      {notice ? <div className="alert ok">{notice}</div> : null}

      {alerts.length === 0 ? (
        <div className="card">
          <p className="muted">Событий нет.</p>
        </div>
      ) : (
        <div className="list">
          {alerts.map((alert) => (
            <div className="card tight" key={alert.id}>
              <div className="row between">
                <div className="row">
                  <SeverityBadge severity={alert.severity} />
                  <b>{alarmTypeLabel(alert.type)}</b>
                  <AlarmStatusBadge status={alert.status} />
                </div>
                <span className="muted small">{formatDateTime(alert.createdAt)}</span>
              </div>
              <div className="row between mt">
                <div>
                  <b>{alert.patientName}</b>
                  <span className="muted small mono"> · {alert.publicCode}</span>
                  {alert.payload ? (
                    <div className="muted small mono">{formatPayload(alert.payload)}</div>
                  ) : null}
                  {alert.comment ? (
                    <div className="muted small">Комментарий: {alert.comment}</div>
                  ) : null}
                </div>
                <div className="row">
                  {alert.patientId ? (
                    <Link className="btn btn-sm" to={`/cabinet/patients/${alert.patientId}`}>
                      Карточка
                    </Link>
                  ) : null}
                  {alert.status !== "CLOSED" ? (
                    <>
                      <button className="btn btn-sm" onClick={() => void act(alert.id, false)}>
                        В работу
                      </button>
                      <button className="btn btn-sm" onClick={() => void act(alert.id, true)}>
                        Закрыть
                      </button>
                    </>
                  ) : null}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
