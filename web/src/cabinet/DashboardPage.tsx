import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { AlarmStatusBadge, ErrorBox, RiskBadge, SeverityBadge, Spinner } from "../components/ui";
import { GettingStartedCard } from "../components/GettingStartedCard";
import { request } from "../api";
import {
  alarmTypeLabel,
  appointmentStatusClass,
  appointmentStatusLabel,
  appointmentTypeLabel,
  appointmentWhen,
  formatDate,
  formatDateTime,
  patientResponseClass,
  patientResponseLabel
} from "../format";
import { useStaff } from "../session";
import type {
  AlertView,
  AppointmentAttentionView,
  DashboardView,
  PatientSummary
} from "../types";

export default function DashboardPage() {
  const { token, me } = useStaff();
  const [dashboard, setDashboard] = useState<DashboardView | null>(null);
  const [alerts, setAlerts] = useState<AlertView[]>([]);
  const [patients, setPatients] = useState<PatientSummary[]>([]);
  const [attention, setAttention] = useState<AppointmentAttentionView | null>(null);
  const [outcomeBusy, setOutcomeBusy] = useState<number | null>(null);
  const [outcomeError, setOutcomeError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    let cancelled = false;
    Promise.all([
      request<DashboardView>("/api/doctor/dashboard", { token }),
      request<AlertView[]>("/api/doctor/alerts?status=NEW", { token }),
      request<PatientSummary[]>("/api/doctor/patients", { token }),
      request<AppointmentAttentionView>("/api/doctor/appointments/attention", { token })
    ])
      .then(([dash, alertList, patientList, attentionView]) => {
        if (!cancelled) {
          setDashboard(dash);
          setAlerts(alertList);
          setPatients(patientList);
          setAttention(attentionView);
          setError(null);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : "Не удалось загрузить обзор");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  if (loading) {
    return <Spinner />;
  }
  if (error || !dashboard) {
    return <ErrorBox message={error ?? "Нет данных"} />;
  }

  const highRisk = patients.filter((patient) => patient.riskGroup === "HIGH");

  const markAppointment = async (id: number, outcome: "complete" | "no-show") => {
    if (!token) {
      return;
    }
    setOutcomeBusy(id);
    setOutcomeError(null);
    try {
      await request(`/api/doctor/appointments/${id}/${outcome}`, {
        method: "POST",
        token
      });
      const updated = await request<AppointmentAttentionView>(
        "/api/doctor/appointments/attention",
        { token }
      );
      setAttention(updated);
    } catch (e) {
      setOutcomeError(e instanceof Error ? e.message : "Не удалось отметить приём");
    } finally {
      setOutcomeBusy(null);
    }
  };

  return (
    <>
      <div className="page-head row between mb">
        <h1 style={{ fontSize: 24 }}>Обзор</h1>
        <Link className="btn btn-primary" to="/cabinet/patients">
          Пациентки
        </Link>
      </div>

      <div className="grid three mb">
        <div className="stat">
          <span className="muted small">Активные пациентки</span>
          <b>{dashboard.patients}</b>
        </div>
        <div className="stat">
          <span className="muted small">Новые события</span>
          <b>{dashboard.openAlerts}</b>
        </div>
        <div className="stat">
          <span className="muted small">Дневник сегодня</span>
          <b>
            {dashboard.diaryToday}
            <span className="muted small"> / {dashboard.patients}</span>
          </b>
        </div>
      </div>

      {attention ? (
        <div className="card">
          <div className="row between">
            <h2>Сегодня</h2>
            <Link className="btn btn-ghost btn-sm" to="/cabinet/calendar">
              Календарь
            </Link>
          </div>
          {attention.today.length === 0 ? (
            <p className="muted mt">На сегодня приёмов нет.</p>
          ) : (
            <div className="calendar-event-list mt">
              {attention.today.map((appointment) => (
                <div className="calendar-event-row" key={appointment.id}>
                  <div className="row between">
                    <div>
                      <b className="calendar-event-title">
                        {appointment.time.slice(0, 5)} · {appointmentTypeLabel(appointment.type)}
                      </b>
                      <div className="muted small">
                        {appointment.patientId ? (
                          <Link to={`/cabinet/patients/${appointment.patientId}`}>
                            {appointment.patientName ?? "Пациентка"}
                          </Link>
                        ) : (
                          "Без пациентки"
                        )}
                        {appointment.place ? ` · ${appointment.place}` : ""}
                      </div>
                    </div>
                    <span
                      className={`badge ${
                        appointment.status === "SCHEDULED"
                          ? patientResponseClass(appointment.patientResponse)
                          : appointmentStatusClass(appointment.status)
                      }`}
                    >
                      {appointment.status === "SCHEDULED"
                        ? patientResponseLabel(appointment.patientResponse)
                        : appointmentStatusLabel(appointment.status)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}

          {attention.pendingOutcome.length > 0 ? (
            <div className="mt">
              <h3 style={{ fontSize: 14 }}>Требуют отметки</h3>
              {outcomeError ? <ErrorBox message={outcomeError} /> : null}
              <div className="calendar-event-list mt">
                {attention.pendingOutcome.map((appointment) => (
                  <div className="calendar-event-row" key={appointment.id}>
                    <div className="row between">
                      <div>
                        <b>{appointmentWhen(appointment.date, appointment.time)}</b>
                        <div className="muted small">
                          {appointment.patientName ?? "Пациентка"} ·{" "}
                          {appointmentTypeLabel(appointment.type)}
                        </div>
                      </div>
                      <div className="row">
                        <button
                          className="btn btn-primary btn-sm"
                          type="button"
                          disabled={outcomeBusy === appointment.id}
                          onClick={() => void markAppointment(appointment.id, "complete")}
                        >
                          Пришла
                        </button>
                        <button
                          className="btn btn-soft-danger btn-sm"
                          type="button"
                          disabled={outcomeBusy === appointment.id}
                          onClick={() => void markAppointment(appointment.id, "no-show")}
                        >
                          Не пришла
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      ) : null}

      <GettingStartedCard doctorId={me?.id ?? 0} patientsCount={patients.length} />

      <div className="card">
        <div className="row between">
          <h2>Новые события</h2>
          <Link className="btn btn-ghost btn-sm" to="/cabinet/alerts">
            Все события
          </Link>
        </div>
        {alerts.length === 0 ? (
          <p className="muted mt">Новых событий нет.</p>
        ) : (
          <>
            <div className="table-wrap dashboard-alerts-table mt">
            <table>
              <thead>
                <tr>
                  <th>Время</th>
                  <th>Пациентка</th>
                  <th>Тип</th>
                  <th>Важность</th>
                  <th>Статус</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {alerts.slice(0, 8).map((alert) => (
                  <tr key={alert.id}>
                    <td>{formatDateTime(alert.createdAt)}</td>
                    <td>
                      {alert.patientName}
                      <div className="muted small mono">{alert.publicCode}</div>
                    </td>
                    <td>{alarmTypeLabel(alert.type)}</td>
                    <td>
                      <SeverityBadge severity={alert.severity} />
                    </td>
                    <td>
                      <AlarmStatusBadge status={alert.status} />
                    </td>
                    <td>
                      {alert.patientId ? (
                        <Link className="btn btn-sm" to={`/cabinet/patients/${alert.patientId}`}>
                          Открыть
                        </Link>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="alerts-cards mt">
            {alerts.slice(0, 8).map((alert) => (
              <div className="list-item" key={alert.id}>
                <div className="row between">
                  <b>{alert.patientName}</b>
                  <SeverityBadge severity={alert.severity} />
                </div>
                <div className="muted small" style={{ marginTop: 4 }}>
                  {formatDateTime(alert.createdAt)} · {alarmTypeLabel(alert.type)}
                </div>
                <div className="row between mt">
                  <AlarmStatusBadge status={alert.status} />
                  {alert.patientId ? (
                    <Link className="btn btn-sm" to={`/cabinet/patients/${alert.patientId}`}>
                      Открыть
                    </Link>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
          </>
        )}
      </div>

      <div className="card">
        <h2>Группа высокого риска</h2>
        {highRisk.length === 0 ? (
          <p className="muted mt">Пациенток с высоким риском нет.</p>
        ) : (
          <div className="list mt">
            {highRisk.map((patient) => (
              <div className="list-item" key={patient.id}>
                <div className="row between">
                  <div>
                    <b>{patient.fullName}</b>
                    <div className="muted small">
                      {patient.gestationalAge} · ПДР {formatDate(patient.edd)}
                    </div>
                  </div>
                  <div className="row">
                    <RiskBadge risk={patient.riskGroup} />
                    <Link className="btn btn-sm" to={`/cabinet/patients/${patient.id}`}>
                      Карточка
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
