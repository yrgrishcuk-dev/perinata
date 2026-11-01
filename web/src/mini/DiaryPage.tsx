import { useCallback, useEffect, useState } from "react";
import { ErrorBox, Field, Spinner } from "../components/ui";
import { request } from "../api";
import { diaryObservationsSummary, formatDate, todayIso } from "../format";
import { usePatient } from "../session";
import type {
  AbdominalPain,
  DiarySaveResponse,
  DiaryView,
  DischargeAmount,
  DischargeCharacter,
  FetalMovements,
  GlucoseContext,
  Headache,
  LegEdema,
  SleepQuality
} from "../types";

function asNumber(value: string): number | null {
  if (!value.trim()) {
    return null;
  }
  const parsed = Number(value.replace(",", "."));
  return Number.isFinite(parsed) ? parsed : null;
}

export default function DiaryPage() {
  const { me, token, refresh } = usePatient();
  const [entries, setEntries] = useState<DiaryView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const [sbp, setSbp] = useState("");
  const [dbp, setDbp] = useState("");
  const [pulse, setPulse] = useState("");
  const [weight, setWeight] = useState("");
  const [glucose, setGlucose] = useState("");
  const [glucoseContext, setGlucoseContext] = useState<GlucoseContext>("FASTING");
  const [note, setNote] = useState("");

  const [dischargeCharacter, setDischargeCharacter] = useState<DischargeCharacter | "">("");
  const [dischargeAmount, setDischargeAmount] = useState<DischargeAmount | "">("");
  const [dischargeOdor, setDischargeOdor] = useState<"" | "yes" | "no">("");
  const [vaginalPh, setVaginalPh] = useState("");
  const [fetalMovements, setFetalMovements] = useState<FetalMovements>("NOT_TRACKED");
  const [lowerAbdominalPain, setLowerAbdominalPain] = useState<AbdominalPain | "">("");
  const [painIntensity, setPainIntensity] = useState("");
  const [legEdema, setLegEdema] = useState<LegEdema | "">("");
  const [sleepQuality, setSleepQuality] = useState<SleepQuality | "">("");
  const [headache, setHeadache] = useState<Headache | "">("");

  const [message, setMessage] = useState<string | null>(null);
  const [severity, setSeverity] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [history, today] = await Promise.all([
        request<DiaryView[]>("/api/patient/diary", { token }),
        request<DiaryView | null>("/api/patient/diary/today", { token })
      ]);
      setEntries(history);
      if (today) {
        setSbp(today.sbp != null ? String(today.sbp) : "");
        setDbp(today.dbp != null ? String(today.dbp) : "");
        setPulse(today.pulse != null ? String(today.pulse) : "");
        setWeight(today.weightKg != null ? String(today.weightKg) : "");
        setGlucose(today.glucoseMmol != null ? String(today.glucoseMmol) : "");
        setGlucoseContext(today.glucoseContext ?? "FASTING");
        setNote(today.note ?? "");
        setDischargeCharacter(today.dischargeCharacter ?? "");
        setDischargeAmount(today.dischargeAmount ?? "");
        setDischargeOdor(
          today.dischargeOdor == null ? "" : today.dischargeOdor ? "yes" : "no"
        );
        setVaginalPh(today.vaginalPh != null ? String(today.vaginalPh) : "");
        setFetalMovements(today.fetalMovements ?? "NOT_TRACKED");
        setLowerAbdominalPain(today.lowerAbdominalPain ?? "");
        setPainIntensity(today.painIntensity != null ? String(today.painIntensity) : "");
        setLegEdema(today.legEdema ?? "");
        setSleepQuality(today.sleepQuality ?? "");
        setHeadache(today.headache ?? "");
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить дневник");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

  if (!me?.home) {
    return null;
  }
  if (loading) {
    return <Spinner />;
  }
  if (error) {
    return <ErrorBox message={error} onRetry={() => void load()} />;
  }

  const diabetes = me.home.summary.diabetesType !== "NONE";
  const showMovements = me.home.summary.gestationalWeek >= 20;
  const hasDischarge =
    dischargeCharacter !== "" && dischargeCharacter !== "NONE";
  const hasPain = lowerAbdominalPain !== "" && lowerAbdominalPain !== "NONE";

  const save = async () => {
    if (!token) {
      return;
    }
    if (hasPain && asNumber(painIntensity) == null) {
      setError("Укажите интенсивность боли");
      return;
    }
    setSaving(true);
    setError(null);
    setMessage(null);
    setSeverity(null);
    try {
      const body = {
        date: todayIso(),
        sbp: asNumber(sbp),
        dbp: asNumber(dbp),
        pulse: asNumber(pulse),
        weightKg: asNumber(weight),
        glucoseMmol: diabetes ? asNumber(glucose) : null,
        glucoseContext: diabetes && asNumber(glucose) != null ? glucoseContext : null,
        note: note.trim() || null,
        dischargeCharacter: dischargeCharacter || null,
        dischargeAmount: hasDischarge && dischargeAmount ? dischargeAmount : null,
        dischargeOdor:
          hasDischarge && dischargeOdor ? dischargeOdor === "yes" : null,
        vaginalPh: asNumber(vaginalPh),
        fetalMovements: showMovements ? fetalMovements : null,
        lowerAbdominalPain: lowerAbdominalPain || null,
        painIntensity: hasPain ? asNumber(painIntensity) : null,
        legEdema: legEdema || null,
        sleepQuality: sleepQuality || null,
        headache: headache || null
      };
      const result = await request<DiarySaveResponse>("/api/patient/diary", {
        method: "POST",
        body,
        token
      });
      setMessage(result.message);
      setSeverity(result.alert?.severity ?? "OK");
      await load();
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось сохранить дневник");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="card">
        <h1 style={{ fontSize: 21, marginBottom: 8 }}>Дневник за сегодня</h1>
        <p className="muted" style={{ marginBottom: 14 }}>
          {formatDate(todayIso())} · заполняйте каждый день, даже если всё хорошо
        </p>

        <div className="grid two">
          <Field label="Верхнее давление, мм рт. ст.">
            <input inputMode="numeric" value={sbp} onChange={(e) => setSbp(e.target.value)} placeholder="120" />
          </Field>
          <Field label="Нижнее давление, мм рт. ст.">
            <input inputMode="numeric" value={dbp} onChange={(e) => setDbp(e.target.value)} placeholder="80" />
          </Field>
          <Field label="Пульс, уд/мин">
            <input inputMode="numeric" value={pulse} onChange={(e) => setPulse(e.target.value)} placeholder="76" />
          </Field>
          <Field label="Вес, кг">
            <input inputMode="decimal" value={weight} onChange={(e) => setWeight(e.target.value)} placeholder="63.2" />
          </Field>
        </div>

        {diabetes ? (
          <div className="grid two">
            <Field label="Глюкоза, ммоль/л">
              <input
                inputMode="decimal"
                value={glucose}
                onChange={(e) => setGlucose(e.target.value)}
                placeholder="4.8"
              />
            </Field>
            <Field label="Когда измеряли">
              <select
                value={glucoseContext}
                onChange={(e) => setGlucoseContext(e.target.value as GlucoseContext)}
              >
                <option value="FASTING">Натощак</option>
                <option value="POSTPRANDIAL">Через 2 часа после еды</option>
              </select>
            </Field>
          </div>
        ) : null}

        <h2 style={{ fontSize: 16, marginTop: 16 }}>Наблюдения</h2>

        <div className="grid two">
          <Field label="Выделения из половых путей">
            <select
              value={dischargeCharacter}
              onChange={(event) => {
                const value = event.target.value as DischargeCharacter | "";
                setDischargeCharacter(value);
                if (!value || value === "NONE") {
                  setDischargeAmount("");
                  setDischargeOdor("");
                }
              }}
            >
              <option value="">Не заполнено</option>
              <option value="NONE">Нет</option>
              <option value="NORMAL">Обычные</option>
              <option value="MUCOUS">Слизистые</option>
              <option value="WATERY">Водянистые</option>
              <option value="BLOODY">Кровянистые</option>
            </select>
          </Field>
          <Field label="Количество">
            <select
              value={dischargeAmount}
              disabled={!hasDischarge}
              onChange={(event) => setDischargeAmount(event.target.value as DischargeAmount | "")}
            >
              <option value="">Не заполнено</option>
              <option value="NORMAL">Обычно</option>
              <option value="ABUNDANT">Обильно</option>
            </select>
          </Field>
          <Field label="Запах">
            <select
              value={dischargeOdor}
              disabled={!hasDischarge}
              onChange={(event) => setDischargeOdor(event.target.value as "" | "yes" | "no")}
            >
              <option value="">Не заполнено</option>
              <option value="no">Нет</option>
              <option value="yes">Есть</option>
            </select>
          </Field>
          <Field label="pH влагалища (кольпо-тест)">
            <input
              inputMode="decimal"
              value={vaginalPh}
              onChange={(event) => setVaginalPh(event.target.value)}
              placeholder="4.5"
            />
          </Field>
        </div>

        {showMovements ? (
          <Field label="Шевеления плода">
            <select
              value={fetalMovements}
              onChange={(event) => setFetalMovements(event.target.value as FetalMovements)}
            >
              <option value="NORMAL">Обычные</option>
              <option value="LESS">Меньше обычного</option>
              <option value="NONE">Нет</option>
              <option value="NOT_TRACKED">Не отслеживала</option>
            </select>
          </Field>
        ) : null}

        <div className="grid two">
          <Field label="Боли внизу живота">
            <select
              value={lowerAbdominalPain}
              onChange={(event) => {
                const value = event.target.value as AbdominalPain | "";
                setLowerAbdominalPain(value);
                if (!value || value === "NONE") {
                  setPainIntensity("");
                }
              }}
            >
              <option value="">Не заполнено</option>
              <option value="NONE">Нет</option>
              <option value="PULLING">Тянущие</option>
              <option value="CRAMPING">Схваткообразные</option>
              <option value="SHARP">Острые</option>
              <option value="OTHER">Другое</option>
            </select>
          </Field>
          <Field label="Интенсивность боли, 0–10">
            <input
              type="number"
              min={0}
              max={10}
              value={painIntensity}
              disabled={!hasPain}
              onChange={(event) => setPainIntensity(event.target.value)}
              placeholder="0"
            />
          </Field>
        </div>

        <Field label="Отёки нижних конечностей">
          <select value={legEdema} onChange={(event) => setLegEdema(event.target.value as LegEdema | "")}>
            <option value="">Не заполнено</option>
            <option value="NONE">Нет</option>
            <option value="MILD">Небольшие</option>
            <option value="PRONOUNCED">Выраженные</option>
            <option value="ASYMMETRIC">Асимметричные</option>
          </select>
        </Field>

        <div className="grid two">
          <Field label="Качество сна">
            <select
              value={sleepQuality}
              onChange={(event) => setSleepQuality(event.target.value as SleepQuality | "")}
            >
              <option value="">Не заполнено</option>
              <option value="GOOD">Хороший</option>
              <option value="NORMAL">Обычный</option>
              <option value="RESTLESS">Беспокойный</option>
              <option value="POOR">Плохой</option>
            </select>
          </Field>
          <Field label="Головная боль">
            <select
              value={headache}
              onChange={(event) => setHeadache(event.target.value as Headache | "")}
            >
              <option value="">Не заполнено</option>
              <option value="NONE">Нет</option>
              <option value="MILD">Лёгкая</option>
              <option value="MODERATE">Умеренная</option>
              <option value="SEVERE">Сильная</option>
            </select>
          </Field>
        </div>
        <p className="notice">
          При умеренной или сильной головной боли измерьте давление. При давлении 140/90 и
          выше — срочно свяжитесь с врачом.
        </p>

        <Field label="Заметка (необязательно)">
          <textarea value={note} onChange={(e) => setNote(e.target.value)} placeholder="Самочувствие, вопросы врачу" />
        </Field>

        {message ? (
          <div className={`alert ${severity === "CRITICAL" ? "danger" : severity === "WARNING" ? "warn" : "ok"}`}>
            {message}
          </div>
        ) : null}

        <button className="btn btn-primary btn-block btn-lg" disabled={saving} onClick={() => void save()}>
          {saving ? "Сохраняем…" : "Сохранить дневник"}
        </button>
      </div>

      <div className="card">
        <h2>История</h2>
        {entries.length === 0 ? (
          <p className="muted mt">Записей пока нет.</p>
        ) : (
          <div className="table-wrap mt">
            <table>
              <thead>
                <tr>
                  <th>Дата</th>
                  <th>АД</th>
                  <th>Пульс</th>
                  <th>Вес</th>
                  <th>Глюкоза</th>
                  <th>Наблюдения</th>
                </tr>
              </thead>
              <tbody>
                {entries
                  .slice()
                  .reverse()
                  .map((entry) => (
                    <tr key={entry.id}>
                      <td>{formatDate(entry.date)}</td>
                      <td>{entry.sbp != null || entry.dbp != null ? `${entry.sbp ?? "—"}/${entry.dbp ?? "—"}` : "—"}</td>
                      <td>{entry.pulse ?? "—"}</td>
                      <td>{entry.weightKg ?? "—"}</td>
                      <td>
                        {entry.glucoseMmol != null
                          ? `${entry.glucoseMmol} ${
                              entry.glucoseContext === "POSTPRANDIAL" ? "после еды" : "натощак"
                            }`
                          : "—"}
                      </td>
                      <td>{diaryObservationsSummary(entry) || "—"}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
