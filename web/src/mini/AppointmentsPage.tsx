import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { AppointmentCard } from "../components/AppointmentCard";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { usePatient } from "../session";
import type { AppointmentView, PatientResponse } from "../types";

export default function AppointmentsPage() {
  const { token, refresh } = usePatient();
  const [items, setItems] = useState<AppointmentView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    setLoading(true);
    request<AppointmentView[]>("/api/patient/appointments", { token })
      .then((data) => {
        setItems(data);
        setError(null);
      })
      .catch((e: unknown) =>
        setError(e instanceof Error ? e.message : "Не удалось загрузить приёмы")
      )
      .finally(() => setLoading(false));
  }, [token]);

  const respond = async (
    appointment: AppointmentView,
    response: Extract<PatientResponse, "CONFIRMED" | "DECLINED">
  ) => {
    if (!token) {
      return;
    }
    setBusyId(appointment.id);
    setError(null);
    try {
      const updated = await request<AppointmentView>(
        `/api/patient/appointments/${appointment.id}/${
          response === "CONFIRMED" ? "confirm" : "decline"
        }`,
        { method: "POST", token }
      );
      setItems((current) =>
        current.map((item) => (item.id === updated.id ? updated : item))
      );
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось сохранить ответ");
    } finally {
      setBusyId(null);
    }
  };

  if (loading) {
    return <Spinner />;
  }

  const now = Date.now();
  const upcoming = items
    .filter(
      (item) =>
        item.status === "SCHEDULED" && new Date(item.startAt).getTime() >= now
    )
    .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime());
  const past = items
    .filter(
      (item) =>
        item.status !== "SCHEDULED" || new Date(item.startAt).getTime() < now
    )
    .sort((a, b) => new Date(b.startAt).getTime() - new Date(a.startAt).getTime());

  return (
    <>
      <div className="row between mb">
        <div>
          <p className="muted small">Расписание</p>
          <h1 style={{ fontSize: 22 }}>Приёмы</h1>
        </div>
        <Link className="btn btn-ghost btn-sm" to="/app">
          На главную
        </Link>
      </div>

      {error ? <ErrorBox message={error} /> : null}

      <div className="card">
        <h2>Предстоящие</h2>
        {upcoming.length === 0 ? (
          <p className="muted mt">Предстоящих приёмов нет.</p>
        ) : (
          <div className="appointment-list mt">
            {upcoming.map((appointment) => (
              <AppointmentCard
                key={appointment.id}
                appointment={appointment}
                busy={busyId === appointment.id}
                onRespond={(response) => void respond(appointment, response)}
              />
            ))}
          </div>
        )}
      </div>

      <div className="card">
        <h2>Прошедшие и отменённые</h2>
        {past.length === 0 ? (
          <p className="muted mt">История приёмов пуста.</p>
        ) : (
          <div className="appointment-list mt">
            {past.map((appointment) => (
              <AppointmentCard key={appointment.id} appointment={appointment} />
            ))}
          </div>
        )}
      </div>
    </>
  );
}
