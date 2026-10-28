import { useEffect, useMemo, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { MiniShell } from "../components/MiniShell";
import { ErrorBox, Field, Spinner } from "../components/ui";
import { request } from "../api";
import { bmiCategoryLabel } from "../format";
import { usePatient } from "../session";
import type { OnboardingInput, ProfileResponse } from "../types";

function categoryFor(bmi: number): "UNDERWEIGHT" | "NORMAL" | "OVERWEIGHT" | "OBESE" {
  if (bmi < 18.5) {
    return "UNDERWEIGHT";
  }
  if (bmi < 25) {
    return "NORMAL";
  }
  if (bmi < 30) {
    return "OVERWEIGHT";
  }
  return "OBESE";
}

export default function OnboardingPage() {
  const { me, token, loading, error, refresh } = usePatient();
  const navigate = useNavigate();

  const [height, setHeight] = useState("");
  const [weight, setWeight] = useState("");
  const [reminder, setReminder] = useState("09:00");
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [profileLoaded, setProfileLoaded] = useState(false);

  useEffect(() => {
    if (!token) {
      return;
    }
    request<ProfileResponse>("/api/patient/profile", { token })
      .then((profile) => {
        if (profile.heightCm != null) {
          setHeight(String(profile.heightCm));
        }
        if (profile.prePregnancyWeightKg != null) {
          setWeight(String(profile.prePregnancyWeightKg));
        }
        if (profile.reminderTime) {
          setReminder(profile.reminderTime.slice(0, 5));
        }
      })
      .catch(() => {
        // профиль может быть недоступен до привязки — форма всё равно откроется
      })
      .finally(() => setProfileLoaded(true));
  }, [token]);

  const bmi = useMemo(() => {
    const h = Number(height.replace(",", "."));
    const w = Number(weight.replace(",", "."));
    if (!h || !w) {
      return null;
    }
    return Math.round((w / Math.pow(h / 100, 2)) * 10) / 10;
  }, [height, weight]);

  if (loading) {
    return (
      <MiniShell subtitle="Настройка">
        <Spinner />
      </MiniShell>
    );
  }
  if (error) {
    return (
      <MiniShell subtitle="Настройка">
        <ErrorBox message={error} onRetry={() => void refresh()} />
      </MiniShell>
    );
  }
  if (!token) {
    return (
      <MiniShell subtitle="Настройка">
        <ErrorBox message="Нет активной сессии. Откройте приложение из Telegram." />
      </MiniShell>
    );
  }
  if (!profileLoaded) {
    return (
      <MiniShell subtitle="Настройка">
        <Spinner />
      </MiniShell>
    );
  }
  if (!me?.bound) {
    return <Navigate to="/connect" replace />;
  }

  const submit = async () => {
    if (!token) {
      return;
    }
    const h = Number(height.replace(",", "."));
    const w = Number(weight.replace(",", "."));
    if (!h || !w) {
      setFormError("Укажите рост и вес до беременности");
      return;
    }
    setBusy(true);
    setFormError(null);
    try {
      const body: OnboardingInput = {
        heightCm: h,
        prePregnancyWeightKg: w,
        reminderTime: reminder
      };
      await request("/api/patient/onboarding", { method: "POST", body, token });
      await refresh();
      navigate("/app", { replace: true });
    } catch (e) {
      setFormError(e instanceof Error ? e.message : "Не удалось сохранить данные");
    } finally {
      setBusy(false);
    }
  };

  return (
    <MiniShell subtitle="Настройка">
      <div className="card">
        <h1 style={{ fontSize: 22, marginBottom: 8 }}>Рост и вес до беременности</h1>
        <p className="muted" style={{ marginBottom: 16 }}>
          По этим данным рассчитаем ИМТ и подберём рекомендуемый коридор прибавки веса и рацион.
        </p>
        <Field label="Рост, см">
          <input
            inputMode="decimal"
            value={height}
            onChange={(event) => setHeight(event.target.value)}
            placeholder="165"
          />
        </Field>
        <Field label="Вес до беременности, кг">
          <input
            inputMode="decimal"
            value={weight}
            onChange={(event) => setWeight(event.target.value)}
            placeholder="62"
          />
        </Field>
        <Field label="Время напоминания о дневнике">
          <input type="time" value={reminder} onChange={(event) => setReminder(event.target.value)} />
        </Field>

        {bmi ? (
          <div className="alert info">
            ИМТ {bmi.toFixed(1)} — {bmiCategoryLabel(categoryFor(bmi))}
          </div>
        ) : null}
        {formError ? <ErrorBox message={formError} /> : null}

        <button
          className="btn btn-primary btn-block btn-lg"
          disabled={busy}
          onClick={() => void submit()}
        >
          {busy ? "Сохраняем…" : "Сохранить и продолжить"}
        </button>
      </div>
      <div className="card tight">
        <p className="notice">
          Если вы не помните точный вес до беременности, укажите наиболее близкое значение. Врач
          сможет скорректировать его в карточке.
        </p>
      </div>
    </MiniShell>
  );
}
