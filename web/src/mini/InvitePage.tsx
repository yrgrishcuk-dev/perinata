import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { MiniShell } from "../components/MiniShell";
import { ErrorBox, Spinner } from "../components/ui";
import { formatDate } from "../format";
import { request } from "../api";
import { usePatient } from "../session";
import { startParam } from "../telegram";
import type { InvitePreview } from "../types";

export default function InvitePage() {
  const [params] = useSearchParams();
  const inviteToken = params.get("t") ?? startParam();
  const navigate = useNavigate();
  const { me, token, loading: sessionLoading, error: sessionError, refresh } = usePatient();

  const [preview, setPreview] = useState<InvitePreview | null>(null);
  const [consent, setConsent] = useState(false);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token || !inviteToken) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    request<InvitePreview>(`/api/patient/invite/${encodeURIComponent(inviteToken)}`, { token })
      .then((data) => {
        if (!cancelled) {
          setPreview(data);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : "Не удалось открыть приглашение");
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
  }, [token, inviteToken]);

  if (sessionLoading) {
    return (
      <MiniShell subtitle="Приглашение">
        <Spinner />
      </MiniShell>
    );
  }
  if (sessionError) {
    return (
      <MiniShell subtitle="Приглашение">
        <ErrorBox message={sessionError} onRetry={() => void refresh()} />
      </MiniShell>
    );
  }
  if (me?.bound) {
    return (
      <MiniShell subtitle="Приглашение">
        <div className="card">
          <h1 style={{ fontSize: 20, marginBottom: 8 }}>Карточка уже привязана</h1>
          <p className="muted">
            Этот Telegram уже связан с карточкой. Если нужно перейти к другой карточке, попросите
            врача отвязать текущую.
          </p>
          <button
            className="btn btn-primary btn-block mt"
            onClick={async () => {
              await refresh();
              navigate("/app", { replace: true });
            }}
          >
            Открыть приложение
          </button>
        </div>
      </MiniShell>
    );
  }
  if (!inviteToken) {
    return (
      <MiniShell subtitle="Приглашение">
        <div className="card">
          <h1 style={{ fontSize: 20, marginBottom: 8 }}>Ссылка неполная</h1>
          <p className="muted">
            Откройте приглашение по кнопке из бота или отсканируйте QR-код врача ещё раз.
          </p>
          <button className="btn btn-primary btn-block mt" onClick={() => navigate("/app")}>
            Перейти в приложение
          </button>
        </div>
      </MiniShell>
    );
  }
  if (loading) {
    return (
      <MiniShell subtitle="Приглашение">
        <Spinner />
      </MiniShell>
    );
  }
  if (error) {
    return (
      <MiniShell subtitle="Приглашение">
        <ErrorBox message={error} />
        <div className="card">
          <p className="muted">
            Ссылка действует 15 минут и только один раз. Попросите врача показать QR-код ещё раз.
          </p>
        </div>
      </MiniShell>
    );
  }
  if (!preview) {
    return null;
  }

  if (preview.alreadyBoundToThisCard) {
    return (
      <MiniShell subtitle="Приглашение">
        <div className="card">
          <h1 style={{ fontSize: 20, marginBottom: 8 }}>Карточка уже привязана</h1>
          <p className="muted">Можно переходить в приложение.</p>
          <button
            className="btn btn-primary btn-block mt"
            onClick={async () => {
              await refresh();
              navigate("/app", { replace: true });
            }}
          >
            Открыть приложение
          </button>
        </div>
      </MiniShell>
    );
  }

  if (preview.alreadyBound) {
    return (
      <MiniShell subtitle="Приглашение">
        <div className="card">
          <h1 style={{ fontSize: 20, marginBottom: 8 }}>Другая карточка</h1>
          <p className="muted">
            Этот Telegram уже привязан к другой карточке. Обратитесь к врачу, чтобы он отвязал
            старую карточку.
          </p>
        </div>
      </MiniShell>
    );
  }

  const confirm = async () => {
    if (!token) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await request(`/api/patient/invite/${encodeURIComponent(inviteToken)}/confirm`, {
        method: "POST",
        body: { consent: true },
        token
      });
      await refresh();
      navigate("/app", { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось подтвердить привязку");
    } finally {
      setBusy(false);
    }
  };

  return (
    <MiniShell subtitle="Приглашение от врача">
      <div className="card">
        <p className="muted small">Проверьте данные карточки</p>
        <h1 style={{ fontSize: 22, margin: "6px 0 14px" }}>{preview.fullName}</h1>
        <dl className="kv">
          <dt>Возраст</dt>
          <dd>{preview.ageYears} лет</dd>
          <dt>Предполагаемая дата родов</dt>
          <dd>{formatDate(preview.edd)}</dd>
          <dt>Врач</dt>
          <dd>{preview.doctorName ?? "—"}</dd>
        </dl>
      </div>

      {error ? <ErrorBox message={error} /> : null}

      <div className="card">
        <h2>Согласие на обработку данных</h2>
        <p className="muted">
          Приложение обрабатывает данные о состоянии здоровья, чтобы вы могли вести дневник и
          получать рекомендации. Данные хранятся на сервере в Российской Федерации.
        </p>
        <details className="mt">
          <summary className="muted">Полный текст согласия</summary>
          <p className="notice mt">
            Я согласна на обработку специальной категории персональных данных (состояние здоровья)
            оператором — медицинской организацией, в объёме: ФИО, возраст, срок беременности, ПДР,
            рост, вес, артериальное давление, глюкоза, симптомы, данные дневника. Цель — ведение
            беременности и информирование лечащего врача. Согласие действует до отзыва. Я понимаю,
            что приложение не заменяет медицинскую помощь, а при тревожных симптомах нужно звонить
            103 или 112. Версия согласия 2026-10-v1.
          </p>
        </details>
        <label className="check">
          <input
            type="checkbox"
            checked={consent}
            onChange={(event) => setConsent(event.target.checked)}
          />
          <span>Я прочитала и согласна с обработкой данных о состоянии здоровья</span>
        </label>
        <button
          className="btn btn-primary btn-block btn-lg"
          disabled={!consent || busy}
          onClick={() => void confirm()}
        >
          {busy ? "Привязываем…" : "Подтвердить и продолжить"}
        </button>
      </div>
    </MiniShell>
  );
}
