import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ErrorBox, RiskBadge, Spinner } from "../components/ui";
import { request } from "../api";
import { bmiCategoryLabel, formatDate, formatDateTime } from "../format";
import { usePatient } from "../session";
import type { ProfileResponse } from "../types";

export default function ProfilePage() {
  const { token, refresh } = usePatient();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) {
      return;
    }
    request<ProfileResponse>("/api/patient/profile", { token })
      .then((data) => {
        setProfile(data);
        setError(null);
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Не удалось загрузить профиль"))
      .finally(() => setLoading(false));
  }, [token]);

  if (loading) {
    return <Spinner />;
  }
  if (error || !profile) {
    return <ErrorBox message={error ?? "Профиль не найден"} />;
  }

  const summary = profile.summary;

  const revoke = async () => {
    if (
      !token ||
      !window.confirm(
        "Отозвать согласие на обработку данных? Доступ к приложению закроется, врач больше не будет получать данные."
      )
    ) {
      return;
    }
    try {
      await request<void>("/api/patient/consent/revoke", { method: "POST", token });
      await refresh();
      navigate("/connect", { replace: true });
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось отозвать согласие");
    }
  };

  return (
    <>
      <div className="card">
        <div className="row between">
          <div>
            <p className="muted small">Карточка</p>
            <h1 style={{ fontSize: 21 }}>{summary.fullName}</h1>
          </div>
          <span className="badge mono">{summary.publicCode}</span>
        </div>
        <div className="row mt">
          <RiskBadge risk={summary.riskGroup} />
          {summary.bmiCategory ? (
            <span className="badge neutral">{bmiCategoryLabel(summary.bmiCategory)}</span>
          ) : null}
          {summary.diabetesType !== "NONE" ? <span className="badge warn">Диабет</span> : null}
        </div>
        <dl className="kv mt">
          <dt>Возраст</dt>
          <dd>{summary.ageYears} лет</dd>
          <dt>Роды по счёту</dt>
          <dd>{summary.parity ?? "—"}</dd>
          <dt>Срок</dt>
          <dd>{summary.gestationalAge}</dd>
          <dt>ПДР</dt>
          <dd>{formatDate(summary.edd)}</dd>
          <dt>Врач</dt>
          <dd>{profile.doctorName ?? "—"}</dd>
          <dt>Рост</dt>
          <dd>{profile.heightCm != null ? `${profile.heightCm} см` : "—"}</dd>
          <dt>Вес до беременности</dt>
          <dd>{profile.prePregnancyWeightKg != null ? `${profile.prePregnancyWeightKg} кг` : "—"}</dd>
          <dt>Напоминание</dt>
          <dd>{profile.reminderTime.slice(0, 5)}</dd>
        </dl>
      </div>

      <div className="card">
        <h2>Данные и согласие</h2>
        <p className="muted mt">
          Версия согласия: {profile.consentVersion}
          <br />
          Дано: {formatDateTime(profile.consentAt)}
        </p>
        <Link className="btn btn-block mt" to="/onboarding">
          Изменить рост, вес и напоминание
        </Link>
        <Link className="btn btn-ghost btn-block" to="/app/messages">
          Сообщения врачу
        </Link>
      </div>

      {actionError ? <ErrorBox message={actionError} /> : null}

      <div className="card">
        <h2>Согласие</h2>
        <p className="muted mt">
          Отзыв согласия прекращает обработку данных в приложении и закрывает доступ. Данные,
          которые уже внесены в медицинскую документацию клиники, регулируются отдельно.
        </p>
        <button className="btn btn-soft-danger btn-block mt" onClick={() => void revoke()}>
          Отозвать согласие
        </button>
      </div>

      <div className="card tight">
        <p className="notice">
          Чтобы отвязать карточку или изменить данные, которые ввёл врач, обратитесь в клинику.
          Приложение не заменяет медицинскую помощь.
        </p>
      </div>
    </>
  );
}
