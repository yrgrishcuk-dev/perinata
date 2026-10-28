import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ErrorBox, RiskBadge, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDate } from "../format";
import { useStaff } from "../session";
import type { PatientSummary } from "../types";
import {
  addDays,
  buildMonthGrid,
  daysBetween,
  daysLabel,
  eddTone,
  formatDayTitle,
  formatMonthTitle,
  isSameDay,
  isSameMonth,
  isWeekend,
  parseIsoDate,
  shortName,
  startOfMonth,
  toIsoDate
} from "../lib/calendar";
import { holidayLabel } from "../lib/holidays";
import { markOnboardingStep } from "../lib/onboarding";

interface PatientEdd {
  patient: PatientSummary;
  edd: Date;
}

const WEEKDAYS = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"];

function sortPatients(items: PatientEdd[]): PatientEdd[] {
  return [...items].sort((a, b) => {
    if (a.patient.riskGroup !== b.patient.riskGroup) {
      return a.patient.riskGroup === "HIGH" ? -1 : 1;
    }
    return a.patient.fullName.localeCompare(b.patient.fullName, "ru");
  });
}

function PatientRow({
  entry,
  reference
}: {
  entry: PatientEdd;
  reference: Date;
}) {
  const days = daysBetween(reference, entry.edd);
  const tone = eddTone(entry.edd, reference);
  return (
    <Link className={`calendar-row ${tone}`} to={`/cabinet/patients/${entry.patient.id}`}>
      <div className="calendar-row-date">
        <span className="calendar-row-dot" />
        {formatDate(entry.patient.edd)}
      </div>
      <div className="calendar-row-main">
        <b>{entry.patient.fullName}</b>
        <span className="muted small">
          {daysLabel(days)} · код {entry.patient.publicCode}
        </span>
      </div>
      <div className="row">
        <RiskBadge risk={entry.patient.riskGroup} />
        {entry.patient.openAlerts > 0 ? (
          <span className="badge danger">{entry.patient.openAlerts}</span>
        ) : null}
      </div>
    </Link>
  );
}

export default function CalendarPage() {
  const { token, me } = useStaff();
  const navigate = useNavigate();
  const today = useMemo(() => {
    const now = new Date();
    return parseIsoDate(toIsoDate(now)) ?? now;
  }, []);
  const [month, setMonth] = useState<Date>(() => startOfMonth(new Date()));
  const [selectedDate, setSelectedDate] = useState<Date>(today);
  const [patients, setPatients] = useState<PatientSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    setLoading(true);
    try {
      const data = await request<PatientSummary[]>("/api/doctor/patients", { token });
      setPatients(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить пациенток");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (me?.id) {
      markOnboardingStep(me.id, "calendar");
    }
  }, [me?.id]);

  const entries = useMemo<PatientEdd[]>(() => {
    return patients.flatMap((patient) => {
      const edd = parseIsoDate(patient.edd);
      return edd ? [{ patient, edd }] : [];
    });
  }, [patients]);

  const monthDays = useMemo(() => buildMonthGrid(month), [month]);

  const eventsFor = useCallback(
    (day: Date) =>
      sortPatients(entries.filter((entry) => isSameDay(entry.edd, day))),
    [entries]
  );

  const windowFor = useCallback(
    (day: Date) =>
      sortPatients(
        entries.filter((entry) => Math.abs(daysBetween(day, entry.edd)) <= 1)
      ),
    [entries]
  );

  const selectedWindow = useMemo(() => windowFor(selectedDate), [windowFor, selectedDate]);

  const upcoming = useMemo(() => {
    return [...entries]
      .filter((entry) => {
        const days = daysBetween(today, entry.edd);
        return days >= -1 && days <= 14;
      })
      .sort((a, b) => a.edd.getTime() - b.edd.getTime());
  }, [entries, today]);

  const changeMonth = (delta: number) => {
    const next = new Date(month.getFullYear(), month.getMonth() + delta, 1, 12, 0, 0, 0);
    setMonth(next);
    setSelectedDate(isSameMonth(today, next)
      ? today
      : new Date(next.getFullYear(), next.getMonth(), 1, 12, 0, 0, 0));
  };

  const goToday = () => {
    setMonth(startOfMonth(today));
    setSelectedDate(today);
  };

  if (loading && patients.length === 0) {
    return <Spinner />;
  }
  if (error && patients.length === 0) {
    return <ErrorBox message={error} onRetry={() => void load()} />;
  }

  return (
    <>
      <div className="row between mb">
        <div>
          <h1 style={{ fontSize: 24 }}>Календарь ПДР</h1>
          <p className="muted small" style={{ marginTop: 4 }}>
            Окно ±1 день · активных пациенток: {patients.length}
          </p>
        </div>
        <div className="row">
          <Link className="btn btn-sm" to="/cabinet/patients">
            Пациентки
          </Link>
          <button className="btn btn-sm" type="button" onClick={() => void load()}>
            Обновить
          </button>
        </div>
      </div>

      {error ? <ErrorBox message={error} /> : null}

      <div className="calendar-layout">
        <div className="card calendar-card">
          <div className="row between calendar-toolbar">
            <button className="btn btn-sm" type="button" onClick={() => changeMonth(-1)}>
              ← Назад
            </button>
            <h2 style={{ fontSize: 18 }}>{formatMonthTitle(month)}</h2>
            <div className="row">
              <button className="btn btn-sm" type="button" onClick={goToday}>
                Сегодня
              </button>
              <button className="btn btn-sm" type="button" onClick={() => changeMonth(1)}>
                Вперёд →
              </button>
            </div>
          </div>

          <div className="calendar-scroll">
            <div className="calendar-weekdays">
              {WEEKDAYS.map((label) => (
                <div key={label} className="calendar-weekday">
                  {label}
                </div>
              ))}
            </div>

            <div className="calendar-grid">
              {monthDays.map((day) => {
                const dayEvents = eventsFor(day);
                const inWindow = entries.some(
                  (entry) => Math.abs(daysBetween(day, entry.edd)) <= 1
                );
                const holiday = holidayLabel(day);
                const classes = [
                  "calendar-day",
                  isSameMonth(day, month) ? "" : "outside",
                  isWeekend(day) ? "weekend" : "",
                  holiday ? "holiday" : "",
                  isSameDay(day, today) ? "today" : "",
                  isSameDay(day, selectedDate) ? "selected" : "",
                  inWindow && dayEvents.length === 0 ? "edd-window" : ""
                ]
                  .filter(Boolean)
                  .join(" ");

                return (
                  <div
                    key={toIsoDate(day)}
                    className={classes}
                    role="button"
                    tabIndex={0}
                    title={holiday ?? undefined}
                    onClick={() => setSelectedDate(day)}
                    onKeyDown={(event) => {
                      if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                        setSelectedDate(day);
                      }
                    }}
                  >
                    <div className="calendar-day-head">
                      <span>{day.getDate()}</span>
                      {holiday ? <span className="calendar-holiday-dot" /> : null}
                    </div>
                    <div className="calendar-events">
                      {dayEvents.slice(0, 3).map((entry) => {
                        const tone = eddTone(entry.edd, today);
                        return (
                          <button
                            key={entry.patient.id}
                            type="button"
                            className={`edd-chip ${tone} ${
                              entry.patient.riskGroup === "HIGH" ? "risk-high" : ""
                            }`}
                            title={`${entry.patient.fullName} · ПДР ${formatDate(entry.patient.edd)}`}
                            onClick={(event) => {
                              event.stopPropagation();
                              navigate(`/cabinet/patients/${entry.patient.id}`);
                            }}
                          >
                            {shortName(entry.patient.fullName)}
                          </button>
                        );
                      })}
                      {dayEvents.length > 3 ? (
                        <span className="calendar-more">+{dayEvents.length - 3}</span>
                      ) : null}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="calendar-legend">
            <span><i className="legend-dot today" />ПДР сегодня</span>
            <span><i className="legend-dot soon" />1–7 дней</span>
            <span><i className="legend-dot upcoming" />8–14 дней</span>
            <span><i className="legend-dot past" />ПДР прошла</span>
            <span><i className="legend-risk" />высокий риск</span>
            <span className="muted">±1 день · выходные и федеральные праздники</span>
          </div>
        </div>

        <aside className="calendar-side">
          <div className="card">
            <h2>На выбранный день</h2>
            <p className="muted small" style={{ marginTop: 4 }}>
              {formatDayTitle(selectedDate)} · ПДР ±1
            </p>
            {selectedWindow.length === 0 ? (
              <p className="muted mt">Нет ПДР в окне ±1 день.</p>
            ) : (
              <div className="calendar-rows">
                {selectedWindow.map((entry) => (
                  <PatientRow key={entry.patient.id} entry={entry} reference={selectedDate} />
                ))}
              </div>
            )}
          </div>

          <div className="card">
            <h2>Ближайшие 14 дней</h2>
            <p className="muted small" style={{ marginTop: 4 }}>
              От вчера до двух недель вперёд.
            </p>
            {upcoming.length === 0 ? (
              <p className="muted mt">На ближайшие две недели ПДР нет.</p>
            ) : (
              <div className="calendar-rows">
                {upcoming.map((entry) => (
                  <PatientRow key={entry.patient.id} entry={entry} reference={today} />
                ))}
              </div>
            )}
          </div>

          <div className="card tight">
            <p className="notice">
              Календарь показывает активные карточки. Если ПДР уже прошла, пациентка остаётся в
              календаре серым — до закрытия наблюдения. Федеральные праздники указаны без переносов
              выходных.
            </p>
          </div>
        </aside>
      </div>
    </>
  );
}
