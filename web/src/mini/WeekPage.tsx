import { useEffect, useState } from "react";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { usePatient } from "../session";
import type { ContentBundle } from "../types";

export default function WeekPage() {
  const { me, token } = usePatient();
  const initialWeek = me?.home?.summary.gestationalWeek ?? 20;
  const [week, setWeek] = useState(Math.max(4, Math.min(42, initialWeek)));
  const [bundle, setBundle] = useState<ContentBundle | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    request<ContentBundle>(`/api/patient/content/${week}`, { token })
      .then((data) => {
        if (!cancelled) {
          setBundle(data);
          setError(null);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : "Не удалось загрузить материал");
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
  }, [token, week]);

  if (loading && !bundle) {
    return <Spinner />;
  }
  if (error && !bundle) {
    return <ErrorBox message={error} />;
  }

  return (
    <>
      <div className="card tight">
        <div className="row between">
          <button
            className="btn btn-sm"
            disabled={week <= 4}
            onClick={() => setWeek((value) => Math.max(4, value - 1))}
          >
            ← Предыдущая
          </button>
          <span className="badge">Неделя {week}</span>
          <button
            className="btn btn-sm"
            disabled={week >= 42}
            onClick={() => setWeek((value) => Math.min(42, value + 1))}
          >
            Следующая →
          </button>
        </div>
      </div>

      {bundle?.content ? (
        <>
          <div className="card">
            <h1 style={{ fontSize: 21, marginBottom: 8 }}>{bundle.content.title}</h1>
            <p style={{ lineHeight: 1.6 }}>{bundle.content.fetalText}</p>
          </div>
          <div className="card">
            <h2>Питание на этой неделе</h2>
            <p className="muted" style={{ marginTop: 8, lineHeight: 1.6 }}>
              {bundle.content.nutritionText}
            </p>
          </div>
          <div className="alert warn">
            <b>Когда обращаться срочно</b>
            <p className="mt">{bundle.content.warningText}</p>
          </div>
        </>
      ) : (
        <div className="card">
          <p className="muted">Материал для этой недели пока не загружен.</p>
        </div>
      )}

      {bundle?.ration ? (
        <div className="card">
          <div className="row between">
            <h2>{bundle.ration.title}</h2>
            <span className="badge">{bundle.ration.kcal} ккал</span>
          </div>
          <p className="muted" style={{ marginTop: 6 }}>
            Белки {bundle.ration.proteinG} г · Жиры {bundle.ration.fatG} г · Углеводы{" "}
            {bundle.ration.carbsG} г
          </p>
          <div className="list mt">
            {bundle.ration.meals.map((meal) => (
              <div className="list-item" key={meal.meal}>
                <b>{meal.meal}</b>
                <p className="muted" style={{ marginTop: 4 }}>
                  {meal.items}
                </p>
              </div>
            ))}
          </div>
          <p className="notice mt">
            Рацион носит информационный характер. Индивидуальные ограничения обсуждайте с врачом.
          </p>
        </div>
      ) : null}
    </>
  );
}
