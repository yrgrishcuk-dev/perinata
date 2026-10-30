import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { appointmentTypeLabel } from "../format";
import { useStaff } from "../session";
import type { AppointmentAnalytics } from "../types";

const PERIODS = [7, 30, 90] as const;

export default function AnalyticsPage() {
  const { token, me } = useStaff();
  const [days, setDays] = useState<number>(30);
  const [doctorId, setDoctorId] = useState("");
  const [data, setData] = useState<AppointmentAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    setLoading(true);
    const query = new URLSearchParams({ days: String(days) });
    if (me?.role === "ADMIN" && doctorId) {
      query.set("doctorId", doctorId);
    }
    request<AppointmentAnalytics>(`/api/doctor/analytics/appointments?${query}`, {
      token
    })
      .then((value) => {
        setData(value);
        setError(null);
      })
      .catch((e: unknown) =>
        setError(e instanceof Error ? e.message : "Не удалось загрузить аналитику")
      )
      .finally(() => setLoading(false));
  }, [token, days, doctorId, me?.role]);

  const maxDaily = useMemo(
    () => Math.max(1, ...(data?.daily.map((point) => point.total) ?? [1])),
    [data]
  );

  if (loading && !data) {
    return <Spinner />;
  }
  if (error || !data) {
    return <ErrorBox message={error ?? "Нет данных"} />;
  }

  const confirmedWidth = data.total ? (data.confirmed / data.total) * 100 : 0;
  const completedWidth = data.total ? (data.completed / data.total) * 100 : 0;
  const noShowWidth = data.total ? (data.noShow / data.total) * 100 : 0;

  return (
    <>
      <div className="page-head row between mb">
        <div>
          <p className="muted small">Приёмы</p>
          <h1 style={{ fontSize: 24 }}>Аналитика</h1>
        </div>
        <div className="row">
          {PERIODS.map((period) => (
            <button
              key={period}
              className={`btn btn-sm ${days === period ? "btn-primary" : ""}`}
              type="button"
              onClick={() => setDays(period)}
            >
              {period} дней
            </button>
          ))}
        </div>
      </div>

      {me?.role === "ADMIN" ? (
        <div className="card">
          <div className="row between">
            <div>
              <h2 style={{ fontSize: 16 }}>Клиника</h2>
              <p className="muted small">
                {data.from} — {data.to}
              </p>
            </div>
            <label className="field" style={{ minWidth: 220, margin: 0 }}>
              <span>Врач</span>
              <select value={doctorId} onChange={(event) => setDoctorId(event.target.value)}>
                <option value="">Все врачи</option>
                {data.doctors.map((doctor) => (
                  <option key={doctor.doctorId} value={doctor.doctorId}>
                    {doctor.fullName}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </div>
      ) : null}

      <div className="grid three mb">
        <div className="stat">
          <span className="muted small">Создано приёмов</span>
          <b>{data.total}</b>
        </div>
        <div className="stat">
          <span className="muted small">Подтверждено</span>
          <b>{data.confirmed}</b>
          <span className="muted small">{data.confirmationRate}%</span>
        </div>
        <div className="stat">
          <span className="muted small">Состоялось</span>
          <b>{data.completed}</b>
          <span className="muted small">{data.showRate}% от завершённых</span>
        </div>
        <div className="stat">
          <span className="muted small">Неявки</span>
          <b>{data.noShow}</b>
          <span className="muted small">{data.noShowRate}%</span>
        </div>
        <div className="stat">
          <span className="muted small">Отмены</span>
          <b>{data.cancelled}</b>
          <span className="muted small">{data.cancelRate}%</span>
        </div>
      </div>

      <div className="card">
        <h2>Воронка</h2>
        <div className="analytics-funnel mt">
          <div className="analytics-funnel-row">
            <span>Создано</span>
            <div className="analytics-track">
              <i style={{ width: "100%" }} />
            </div>
            <b>{data.total}</b>
          </div>
          <div className="analytics-funnel-row">
            <span>Подтверждено</span>
            <div className="analytics-track">
              <i style={{ width: `${confirmedWidth}%` }} />
            </div>
            <b>{data.confirmed}</b>
          </div>
          <div className="analytics-funnel-row">
            <span>Состоялось</span>
            <div className="analytics-track">
              <i style={{ width: `${completedWidth}%` }} />
            </div>
            <b>{data.completed}</b>
          </div>
          <div className="analytics-funnel-row">
            <span>Неявки</span>
            <div className="analytics-track danger">
              <i style={{ width: `${noShowWidth}%` }} />
            </div>
            <b>{data.noShow}</b>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="row between">
          <h2>По дням</h2>
          <span className="muted small">всего / состоялось / неявки</span>
        </div>
        <div className="analytics-chart mt">
          {data.daily.map((point) => {
            const totalHeight = Math.round((point.total / maxDaily) * 160);
            const completedHeight = point.total
              ? Math.round((point.completed / point.total) * totalHeight)
              : 0;
            const noShowHeight = point.total
              ? Math.round((point.noShow / point.total) * totalHeight)
              : 0;
            return (
              <div
                className="analytics-bar"
                key={point.date}
                title={`${point.date}: создано ${point.total}, состоялось ${point.completed}, неявки ${point.noShow}`}
              >
                <div className="analytics-bar-stack" style={{ height: totalHeight }}>
                  <i className="analytics-segment completed" style={{ height: completedHeight }} />
                  <i className="analytics-segment no-show" style={{ height: noShowHeight }} />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="grid two">
        <div className="card">
          <h2>По типам</h2>
          <div className="analytics-types mt">
            {data.byType.map((point) => (
              <div className="analytics-type-row" key={point.type}>
                <div className="row between">
                  <span>{appointmentTypeLabel(point.type)}</span>
                  <b>{point.total}</b>
                </div>
                <div className="analytics-track">
                  <i
                    style={{
                      width: `${data.total ? (point.total / data.total) * 100 : 0}%`
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>

        {me?.role === "ADMIN" ? (
          <div className="card">
            <h2>По врачам</h2>
            <div className="analytics-doctors mt">
              {data.doctors.map((doctor) => (
                <div className="analytics-doctor-row" key={doctor.doctorId}>
                  <div className="row between">
                    <b>{doctor.fullName}</b>
                    <span className="muted small">
                      {doctor.total} приёмов · {doctor.completed} состоялось ·{" "}
                      {doctor.noShow} неявок
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="card">
            <h2>Что дальше</h2>
            <p className="muted mt">
              Отмечайте приёмы «Пришла» и «Не пришла» на обзоре — тогда аналитика будет точной.
            </p>
            <Link className="btn btn-primary mt" to="/cabinet">
              На обзор
            </Link>
          </div>
        )}
      </div>
    </>
  );
}
