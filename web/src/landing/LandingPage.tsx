import { useEffect, useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { request } from "../api";
import { maxApp, telegram } from "../telegram";

interface PublicConfig {
  telegramBotUsername: string;
}

export default function LandingPage() {
  const [config, setConfig] = useState<PublicConfig | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    request<PublicConfig>("/api/public/config")
      .then(setConfig)
      .catch(() => setFailed(true));
  }, []);

  if (telegram()?.initData || maxApp()?.initData) {
    return <Navigate to="/app" replace />;
  }

  const username = config?.telegramBotUsername?.trim() ?? "";
  const loading = !config && !failed;

  return (
    <div className="shell">
      <main className="landing">
        <header className="landing-head">
          <div className="brand">
            Антенатальный помощник
            <small>Наблюдение за беременностью</small>
          </div>
          <p className="muted">Выберите, как продолжить.</p>
        </header>

        <div className="landing-grid">
          <section className="card landing-card">
            <div className="landing-icon" aria-hidden="true">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path
                  d="M9 11a4 4 0 100-8 4 4 0 000 8z"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                />
                <path
                  d="M3 21v-1a6 6 0 016-6h2"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                />
                <path
                  d="M15 18l2 2 4-4"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <h1>Я врач</h1>
            <p className="muted">Кабинет врача и администратора клиники.</p>
            <Link className="btn btn-primary btn-block" to="/cabinet/login">
              Войти в кабинет
            </Link>
          </section>

          <section className="card landing-card">
            <div className="landing-icon" aria-hidden="true">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 20s-7-4.4-7-9.5A4.5 4.5 0 0112 7a4.5 4.5 0 017 3.5C19 15.6 12 20 12 20z"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <h1>Я пациентка</h1>
            <p className="muted">Дневник, симптомы, схватки и развитие плода.</p>
            {username ? (
              <a
                className="btn btn-primary btn-block"
                href={`https://t.me/${username}`}
                target="_blank"
                rel="noopener noreferrer"
              >
                Открыть в Telegram
              </a>
            ) : (
              <button className="btn btn-primary btn-block" type="button" disabled>
                {loading ? "Загрузка…" : "Открыть в Telegram"}
              </button>
            )}
            {failed ? (
              <p className="field-error">Не удалось получить ссылку. Обновите страницу.</p>
            ) : null}
            {!loading && !failed && !username ? (
              <p className="notice">Бот пока не настроен.</p>
            ) : null}
            {username ? (
              <p className="notice">Нужен QR-код или приглашение от врача.</p>
            ) : null}
          </section>
        </div>
      </main>
    </div>
  );
}
