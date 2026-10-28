import { Link } from "react-router-dom";
import { GainChart } from "../components/GainChart";
import { usePatient } from "../session";
import {
  formatDate,
  gainStatusClass,
  gainStatusLabel
} from "../format";
import type { DiaryView } from "../types";

function diarySummary(entry: DiaryView): string {
  const parts: string[] = [];
  if (entry.sbp != null || entry.dbp != null) {
    parts.push(`АД ${entry.sbp ?? "—"}/${entry.dbp ?? "—"}`);
  }
  if (entry.pulse != null) {
    parts.push(`пульс ${entry.pulse}`);
  }
  if (entry.weightKg != null) {
    parts.push(`вес ${entry.weightKg} кг`);
  }
  if (entry.glucoseMmol != null) {
    parts.push(`глюкоза ${entry.glucoseMmol} ммоль/л`);
  }
  return parts.length ? parts.join(" · ") : "Запись без показателей";
}

export default function HomePage() {
  const { me } = usePatient();
  const home = me?.home;
  if (!home) {
    return null;
  }
  const summary = home.summary;
  const gain = home.gain;

  return (
    <>
      <div className="card">
        <div className="row between">
          <div>
            <p className="muted small">Сейчас</p>
            <h1 style={{ fontSize: 22 }}>{summary.gestationalAge}</h1>
            <p className="muted">ПДР {formatDate(summary.edd)}</p>
          </div>
          <div className="center">
            <span className={`badge ${gainStatusClass(gain.status)}`}>
              {gainStatusLabel(gain.status)}
            </span>
            <p className="muted small" style={{ marginTop: 6 }}>
              ИМТ {summary.bmi?.toFixed(1) ?? "—"}
            </p>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="row between">
          <h2>Прибавка веса</h2>
          <Link className="btn btn-ghost btn-sm" to="/app/diary">
            Заполнить
          </Link>
        </div>
        <p className="muted" style={{ margin: "4px 0 8px" }}>
          {gain.gainKg != null ? `Текущая прибавка ${gain.gainKg.toFixed(1)} кг` : "Нет данных о весе"}
          {gain.minGainKg != null && gain.maxGainKg != null
            ? ` · коридор ${gain.minGainKg.toFixed(1)}–${gain.maxGainKg.toFixed(1)} кг`
            : ""}
        </p>
        <GainChart gain={gain} />
      </div>

      <div className="card">
        <div className="row between">
          <h2>Дневник сегодня</h2>
          <Link className="btn btn-ghost btn-sm" to="/app/diary">
            {home.todayDiary ? "Изменить" : "Заполнить"}
          </Link>
        </div>
        <p className="muted" style={{ marginTop: 6 }}>
          {home.todayDiary ? diarySummary(home.todayDiary) : "Пока не заполнен"}
        </p>
      </div>

      <div className="grid two">
        <Link className="card tight" to="/app/symptoms" style={{ textDecoration: "none", color: "inherit" }}>
          <span className="badge danger">Важно</span>
          <h3 className="mt">Тревожные симптомы</h3>
          <p className="muted">Проверить самочувствие за 1 минуту</p>
        </Link>
        <Link className="card tight" to="/app/contractions" style={{ textDecoration: "none", color: "inherit" }}>
          <span className="badge neutral">Таймер</span>
          <h3 className="mt">Счётчик схваток</h3>
          <p className="muted">Когда пора ехать в роддом</p>
        </Link>
      </div>

      {home.week ? (
        <div className="card">
          <div className="row between">
            <h2>{home.week.title}</h2>
            <Link className="btn btn-ghost btn-sm" to="/app/week">
              Читать
            </Link>
          </div>
          <p className="muted" style={{ marginTop: 6 }}>
            {home.week.fetalText.length > 180
              ? `${home.week.fetalText.slice(0, 180)}…`
              : home.week.fetalText}
          </p>
        </div>
      ) : null}

      {home.ration ? (
        <div className="card">
          <div className="row between">
            <h2>{home.ration.title}</h2>
            <span className="badge">{home.ration.kcal} ккал</span>
          </div>
          <p className="muted" style={{ marginTop: 6 }}>
            Б {home.ration.proteinG} г · Ж {home.ration.fatG} г · У {home.ration.carbsG} г
          </p>
          <Link className="btn btn-ghost btn-sm" to="/app/week">
            Рацион и питание
          </Link>
        </div>
      ) : null}

      <div className="card">
        <div className="row between">
          <h2>Связь с врачом</h2>
          <Link className="btn btn-ghost btn-sm" to="/app/messages">
            Сообщения{home.unreadMessages > 0 ? ` (${home.unreadMessages})` : ""}
          </Link>
        </div>
        <p className="muted" style={{ marginTop: 6 }}>
          {home.doctorName ?? "Врач не указан"}
        </p>
        <Link className="btn btn-ghost btn-sm" to="/app/profile">
          Профиль, данные и согласие
        </Link>
      </div>

      <div className="card tight">
        <p className="notice">
          Приложение не заменяет врача. При кровотечении, сильной боли, высокой температуре,
          нарушении зрения или отхождении вод звоните 103 или 112.
        </p>
      </div>
    </>
  );
}
