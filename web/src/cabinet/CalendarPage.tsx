import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { ErrorBox, Field, RiskBadge, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDate } from "../format";
import { useStaff } from "../session";
import type { CalendarEventInput, CalendarEventView, PatientSummary } from "../types";
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

interface EventDraft {
  id?: number;
  date: string;
  time: string;
  title: string;
  patientId: string;
  note: string;
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
  const [calendarEvents, setCalendarEvents] = useState<CalendarEventView[]>([]);
  const [eventsError, setEventsError] = useState<string | null>(null);
  const [eventsReload, setEventsReload] = useState(0);
  const [eventDraft, setEventDraft] = useState<EventDraft | null>(null);
  const [savingEvent, setSavingEvent] = useState(false);
  const [eventFormError, setEventFormError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useSearchParams();

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

  useEffect(() => {
    if (!token) {
      return;
    }
    const grid = buildMonthGrid(month);
    const from = toIsoDate(grid[0]);
    const to = toIsoDate(grid[grid.length - 1]);
    request<CalendarEventView[]>(
      `/api/doctor/calendar/events?from=${from}&to=${to}`,
      { token }
    )
      .then((data) => {
        setCalendarEvents(data);
        setEventsError(null);
      })
      .catch((e: unknown) =>
        setEventsError(e instanceof Error ? e.message : "Не удалось загрузить события")
      );
  }, [token, month, eventsReload]);

  useEffect(() => {
    if (searchParams.get("new") !== "1") {
      return;
    }
    setEventDraft({
      date: searchParams.get("date") ?? toIsoDate(today),
      time: "",
      title: "",
      patientId: searchParams.get("patientId") ?? "",
      note: ""
    });
    setSearchParams({}, { replace: true });
  }, [searchParams, setSearchParams, today]);

  const entries = useMemo<PatientEdd[]>(() => {
    return patients.flatMap((patient) => {
      const edd = parseIsoDate(patient.edd);
      return edd ? [{ patient, edd }] : [];
    });
  }, [patients]);

  const monthDays = useMemo(() => buildMonthGrid(month), [month]);

  const eddFor = useCallback(
    (day: Date) =>
      sortPatients(entries.filter((entry) => isSameDay(entry.edd, day))),
    [entries]
  );

  const calendarEventsFor = useCallback(
    (day: Date) => calendarEvents.filter((event) => event.date === toIsoDate(day)),
    [calendarEvents]
  );

  const windowFor = useCallback(
    (day: Date) =>
      sortPatients(
        entries.filter((entry) => Math.abs(daysBetween(day, entry.edd)) <= 1)
      ),
    [entries]
  );

  const selectedWindow = useMemo(() => windowFor(selectedDate), [windowFor, selectedDate]);

  const selectedEvents = useMemo(
    () => calendarEventsFor(selectedDate),
    [calendarEventsFor, selectedDate]
  );

  const patientOptions = useMemo(
    () => [...patients].sort((a, b) => a.fullName.localeCompare(b.fullName, "ru")),
    [patients]
  );

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

  const openNewEvent = (date: Date) => {
    setEventFormError(null);
    setEventDraft({
      date: toIsoDate(date),
      time: "",
      title: "",
      patientId: "",
      note: ""
    });
  };

  const openEditEvent = (event: CalendarEventView) => {
    setEventFormError(null);
    setEventDraft({
      id: event.id,
      date: event.date,
      time: event.time ? event.time.slice(0, 5) : "",
      title: event.title,
      patientId: event.patientId != null ? String(event.patientId) : "",
      note: event.note ?? ""
    });
  };

  const saveEvent = async () => {
    if (!token || !eventDraft) {
      return;
    }
    if (!eventDraft.title.trim()) {
      setEventFormError("Укажите название события");
      return;
    }
    if (!eventDraft.date) {
      setEventFormError("Укажите дату события");
      return;
    }
    setSavingEvent(true);
    setEventFormError(null);
    try {
      const body: CalendarEventInput = {
        date: eventDraft.date,
        title: eventDraft.title.trim(),
        time: eventDraft.time || null,
        patientId: eventDraft.patientId ? Number(eventDraft.patientId) : null,
        note: eventDraft.note.trim() || null
      };
      await request(
        eventDraft.id
          ? `/api/doctor/calendar/events/${eventDraft.id}`
          : "/api/doctor/calendar/events",
        {
          method: eventDraft.id ? "PUT" : "POST",
          body,
          token
        }
      );
      setEventDraft(null);
      setEventsReload((value) => value + 1);
    } catch (e) {
      setEventFormError(e instanceof Error ? e.message : "Не удалось сохранить событие");
    } finally {
      setSavingEvent(false);
    }
  };

  const deleteEvent = async () => {
    if (!token || !eventDraft?.id) {
      return;
    }
    if (!window.confirm("Удалить событие?")) {
      return;
    }
    setSavingEvent(true);
    setEventFormError(null);
    try {
      await request(`/api/doctor/calendar/events/${eventDraft.id}`, {
        method: "DELETE",
        token
      });
      setEventDraft(null);
      setEventsReload((value) => value + 1);
    } catch (e) {
      setEventFormError(e instanceof Error ? e.message : "Не удалось удалить событие");
    } finally {
      setSavingEvent(false);
    }
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
          <h1 style={{ fontSize: 24 }}>Календарь</h1>
          <p className="muted small" style={{ marginTop: 4 }}>
            ПДР ±1 день · активных пациенток: {patients.length} · событий: {calendarEvents.length}
          </p>
        </div>
        <div className="row">
          <button className="btn btn-primary btn-sm" type="button" onClick={() => openNewEvent(selectedDate)}>
            Добавить событие
          </button>
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
                const dayEdd = eddFor(day);
                const dayCalendarEvents = calendarEventsFor(day);
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
                  inWindow && dayEdd.length === 0 ? "edd-window" : ""
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
                      {dayEdd.slice(0, 2).map((entry) => {
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
                      {dayCalendarEvents.slice(0, 2).map((event) => (
                        <button
                          key={event.id}
                          type="button"
                          className="event-chip"
                          title={`${event.title}${event.patientName ? ` · ${event.patientName}` : ""}`}
                          onClick={(clickEvent) => {
                            clickEvent.stopPropagation();
                            openEditEvent(event);
                          }}
                        >
                          {event.time ? `${event.time.slice(0, 5)} ` : ""}
                          {event.title}
                        </button>
                      ))}
                      {dayEdd.length > 2 || dayCalendarEvents.length > 2 ? (
                        <span className="calendar-more">
                          +{Math.max(0, dayEdd.length - 2) + Math.max(0, dayCalendarEvents.length - 2)}
                        </span>
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
            <span><i className="legend-event" />событие врача</span>
            <span className="muted">±1 день · выходные и федеральные праздники</span>
          </div>
        </div>

        <aside className="calendar-side">
          <div className="card">
            <div className="row between">
              <h2>События</h2>
              <button className="btn btn-sm" type="button" onClick={() => openNewEvent(selectedDate)}>
                Добавить
              </button>
            </div>
            <p className="muted small" style={{ marginTop: 4 }}>
              {formatDayTitle(selectedDate)}
            </p>
            {eventsError ? <ErrorBox message={eventsError} /> : null}
            {selectedEvents.length === 0 ? (
              <p className="muted mt">На этот день событий нет.</p>
            ) : (
              <div className="calendar-event-list mt">
                {selectedEvents.map((event) => (
                  <div className="calendar-event-row" key={event.id}>
                    <div className="row between">
                      <b className="calendar-event-title">
                        {event.time ? `${event.time.slice(0, 5)} · ` : ""}
                        {event.title}
                      </b>
                      <button
                        className="btn btn-ghost btn-sm"
                        type="button"
                        onClick={() => openEditEvent(event)}
                      >
                        Изменить
                      </button>
                    </div>
                    {event.patientId ? (
                      <Link
                        className="muted small"
                        to={`/cabinet/patients/${event.patientId}`}
                      >
                        {event.patientName ?? "Пациентка"} · {event.patientPublicCode}
                      </Link>
                    ) : (
                      <span className="muted small">Без привязки к пациентке</span>
                    )}
                    {event.note ? <p className="muted small mt">{event.note}</p> : null}
                  </div>
                ))}
              </div>
            )}
          </div>

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

      {eventDraft ? (
        <div className="modal-backdrop" onClick={() => setEventDraft(null)}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <h2>{eventDraft.id ? "Событие" : "Новое событие"}</h2>
            <div className="grid two mt">
              <Field label="Дата">
                <input
                  type="date"
                  value={eventDraft.date}
                  onChange={(event) =>
                    setEventDraft({ ...eventDraft, date: event.target.value })
                  }
                />
              </Field>
              <Field label="Время">
                <input
                  type="time"
                  value={eventDraft.time}
                  onChange={(event) =>
                    setEventDraft({ ...eventDraft, time: event.target.value })
                  }
                />
              </Field>
            </div>
            <Field label="Название">
              <input
                value={eventDraft.title}
                onChange={(event) =>
                  setEventDraft({ ...eventDraft, title: event.target.value })
                }
                placeholder="Например, приём или анализ"
                maxLength={200}
              />
            </Field>
            <Field label="Пациентка">
              <select
                value={eventDraft.patientId}
                onChange={(event) =>
                  setEventDraft({ ...eventDraft, patientId: event.target.value })
                }
              >
                <option value="">Без привязки</option>
                {patientOptions.map((patient) => (
                  <option key={patient.id} value={patient.id}>
                    {patient.fullName} · {patient.publicCode}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Заметка">
              <textarea
                value={eventDraft.note}
                onChange={(event) =>
                  setEventDraft({ ...eventDraft, note: event.target.value })
                }
                maxLength={1000}
              />
            </Field>
            {eventFormError ? <ErrorBox message={eventFormError} /> : null}
            <div className="row mt">
              <button
                className="btn btn-primary"
                type="button"
                disabled={savingEvent}
                onClick={() => void saveEvent()}
              >
                {savingEvent ? "Сохраняем…" : "Сохранить"}
              </button>
              {eventDraft.id ? (
                <button
                  className="btn btn-soft-danger"
                  type="button"
                  disabled={savingEvent}
                  onClick={() => void deleteEvent()}
                >
                  Удалить
                </button>
              ) : null}
              <button
                className="btn btn-ghost"
                type="button"
                onClick={() => setEventDraft(null)}
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
