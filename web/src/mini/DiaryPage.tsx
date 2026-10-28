import { useCallback, useEffect, useState } from "react";
import { ErrorBox, Field, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDate, todayIso } from "../format";
import { usePatient } from "../session";
import type { DiarySaveResponse, DiaryView, GlucoseContext } from "../types";

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

  const save = async () => {
    if (!token) {
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
        note: note.trim() || null
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
