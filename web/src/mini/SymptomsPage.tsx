import { useCallback, useEffect, useState } from "react";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDateTime } from "../format";
import { usePatient } from "../session";
import type { SymptomResponse } from "../types";

const SYMPTOMS = [
  { code: "BLEEDING", label: "Кровянистые выделения из половых путей" },
  { code: "LIQUID_DISCHARGE", label: "Обильные жидкие выделения из половых путей" },
  {
    code: "HEADACHE_VISION",
    label: "Постоянная головная боль, нарушение зрения с появлением в глазах пятнышек или вспышек"
  },
  { code: "SUDDEN_EDEMA", label: "Внезапный отек рук, лица, поясницы" },
  { code: "FEVER", label: "Повышение температуры тела до 38 °C" },
  {
    code: "ITCHING",
    label: "Сильный зуд и жжение во влагалище или усилившиеся влагалищные выделения"
  },
  { code: "URINATION_PAIN", label: "Жжение и боль при мочеиспускании" },
  {
    code: "ABDOMINAL_PAIN",
    label: "Сильная боль в животе, не стихающая, даже когда Вы лежите и расслабляетесь"
  },
  { code: "CONTRACTIONS", label: "Более 4–5 схваток за 1 час" }
];

const labelFor = (code: string) => SYMPTOMS.find((item) => item.code === code)?.label ?? code;

export default function SymptomsPage() {
  const { token, refresh } = usePatient();
  const [selected, setSelected] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SymptomResponse | null>(null);
  const [history, setHistory] = useState<SymptomResponse[]>([]);

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    setLoading(true);
    try {
      const data = await request<SymptomResponse[]>("/api/patient/symptoms/history", { token });
      setHistory(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить историю");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading && !result?.critical) {
    return <Spinner />;
  }
  if (error && !result) {
    return <ErrorBox message={error} onRetry={() => void load()} />;
  }

  const checked = Object.keys(selected).filter((code) => selected[code]);

  const submit = async () => {
    if (!token) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const response = await request<SymptomResponse>("/api/patient/symptoms", {
        method: "POST",
        body: { symptoms: checked },
        token
      });
      setResult(response);
      setSelected({});
      await load();
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось отправить анкету");
    } finally {
      setBusy(false);
    }
  };

  if (result?.critical) {
    return (
      <div className="redscreen overlay">
        <div className="red-top">
          <h1>Немедленно обратитесь к врачу</h1>
          <p>{result.message}</p>
        </div>
        <div className="card">
          <h2>Вы отметили</h2>
          <ul style={{ lineHeight: 1.6, paddingLeft: 18 }}>
            {result.symptoms.map((code) => (
              <li key={code}>{labelFor(code)}</li>
            ))}
          </ul>
          <p className="muted" style={{ marginTop: 12 }}>
            Врач получил уведомление. Не оставайтесь дома одна. Возьмите документы и позвоните в
            приёмное отделение.
          </p>
        </div>
        <a className="btn btn-danger btn-block btn-lg" href="tel:103">
          Позвонить 103
        </a>
        <a className="btn btn-block mt" href="tel:112">
          Позвонить 112
        </a>
        <button
          className="btn btn-ghost btn-block mt"
          type="button"
          onClick={() => setResult(null)}
        >
          Вернуться в приложение
        </button>
      </div>
    );
  }

  return (
    <>
      <div className="card">
        <h1 style={{ fontSize: 21, marginBottom: 6 }}>Тревожные симптомы</h1>
        <p className="muted" style={{ marginBottom: 12 }}>
          Отметьте всё, что есть сейчас. Если хотя бы один пункт совпал — приложение покажет, что
          делать, и сообщит врачу.
        </p>
        {SYMPTOMS.map((symptom) => (
          <label className="check" key={symptom.code}>
            <input
              type="checkbox"
              checked={Boolean(selected[symptom.code])}
              onChange={(event) =>
                setSelected((previous) => ({ ...previous, [symptom.code]: event.target.checked }))
              }
            />
            <span>{symptom.label}</span>
          </label>
        ))}
        {error ? <ErrorBox message={error} /> : null}
        {result && !result.critical ? <div className="alert ok">{result.message}</div> : null}
        <button className="btn btn-primary btn-block btn-lg mt" disabled={busy} onClick={() => void submit()}>
          {busy ? "Отправляем…" : "Проверить"}
        </button>
      </div>

      <div className="card">
        <h2>История анкет</h2>
        {history.length === 0 ? (
          <p className="muted mt">Пока нет заполненных анкет.</p>
        ) : (
          <div className="list mt">
            {history.map((item, index) => (
              <div className="list-item" key={`${item.reportedAt}-${index}`}>
                <div className="row between">
                  <b>{formatDateTime(item.reportedAt)}</b>
                  <span className={`badge ${item.critical ? "danger" : "ok"}`}>
                    {item.critical ? "Есть симптомы" : "Без симптомов"}
                  </span>
                </div>
                {item.critical ? (
                  <p className="muted" style={{ marginTop: 6 }}>
                    {item.symptoms.map(labelFor).join("; ")}
                  </p>
                ) : null}
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
