import { useCallback, useEffect, useState } from "react";
import { Link, Navigate, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { useStaff } from "../session";
import type { MessengerLinkResponse, MessengerPlatform, MessengerStatus } from "../types";

const PLATFORM_LABEL: Record<MessengerPlatform, string> = {
  TELEGRAM: "Telegram",
  MAX: "MAX"
};

export default function CabinetLayout() {
  const { me, loading, logout, token } = useStaff();
  const navigate = useNavigate();
  const location = useLocation();
  const [messenger, setMessenger] = useState<MessengerLinkResponse | null>(null);
  const [messengerPlatform, setMessengerPlatform] = useState<MessengerPlatform>("TELEGRAM");
  const [status, setStatus] = useState<MessengerStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [moreOpen, setMoreOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

  const loadStatus = useCallback(async () => {
    if (!token) {
      return;
    }
    try {
      const data = await request<MessengerStatus>("/api/doctor/messenger", { token });
      setStatus(data);
    } catch {
      // статус не критичен для работы кабинета
    }
  }, [token]);

  useEffect(() => {
    void loadStatus();
  }, [loadStatus]);

  useEffect(() => {
    if (!token) {
      return;
    }
    const onFocus = () => {
      void loadStatus();
    };
    window.addEventListener("focus", onFocus);
    return () => window.removeEventListener("focus", onFocus);
  }, [token, loadStatus]);

  if (loading) {
    return (
      <div className="shell">
        <Spinner />
      </div>
    );
  }
  if (!me) {
    return <Navigate to="/cabinet/login" replace />;
  }

  const connectMessenger = async (platform: MessengerPlatform) => {
    if (!token) {
      return;
    }
    setMessengerPlatform(platform);
    setBusy(true);
    try {
      const data = await request<MessengerLinkResponse>(
        `/api/doctor/messenger-link?platform=${platform}`,
        { method: "POST", token }
      );
      setMessenger(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось создать ссылку");
    } finally {
      setBusy(false);
    }
  };

  const unlinkMessenger = async () => {
    if (!token || !status?.linked) {
      return;
    }
    const label = status.platform ? PLATFORM_LABEL[status.platform] : "мессенджера";
    const confirmed = window.confirm(
      `Отвязать уведомления от ${label}? После этого можно подключить другой мессенджер.`
    );
    if (!confirmed) {
      return;
    }
    setBusy(true);
    try {
      const data = await request<MessengerStatus>("/api/doctor/messenger", {
        method: "DELETE",
        token
      });
      setStatus(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось отвязать мессенджер");
    } finally {
      setBusy(false);
    }
  };

  const closeMessenger = () => {
    setMessenger(null);
    void loadStatus();
  };

  const linked = status?.linked === true;
  const linkedLabel = status?.platform ? PLATFORM_LABEL[status.platform] : null;

  const connectButtons = (
    <>
      <button className="btn btn-sm" disabled={busy} onClick={() => void connectMessenger("TELEGRAM")}>
        Подключить Telegram
      </button>
      <button className="btn btn-sm" disabled={busy} onClick={() => void connectMessenger("MAX")}>
        Подключить MAX
      </button>
    </>
  );

  return (
    <div className="cabinet">
      <aside className="sidebar">
        <div className="logo">
          Антенатальный помощник
          <span>Кабинет врача</span>
        </div>
        <NavLink to="/cabinet" end className={({ isActive }) => (isActive ? "active" : "")}>
          Обзор
        </NavLink>
        <NavLink to="/cabinet/patients" className={({ isActive }) => (isActive ? "active" : "")}>
          Пациентки
        </NavLink>
        <NavLink to="/cabinet/calendar" className={({ isActive }) => (isActive ? "active" : "")}>
          Календарь
        </NavLink>
        <NavLink to="/cabinet/alerts" className={({ isActive }) => (isActive ? "active" : "")}>
          События
        </NavLink>
        <NavLink to="/cabinet/analytics" className={({ isActive }) => (isActive ? "active" : "")}>
          Аналитика
        </NavLink>
        <NavLink to="/cabinet/help" className={({ isActive }) => (isActive ? "active" : "")}>
          Обучение
        </NavLink>
        {me.role === "ADMIN" ? (
          <>
            <NavLink to="/cabinet/admin" className={({ isActive }) => (isActive ? "active" : "")}>
              Врачи
            </NavLink>
            <NavLink to="/cabinet/audit" className={({ isActive }) => (isActive ? "active" : "")}>
              Аудит
            </NavLink>
          </>
        ) : null}
        <div className="spacer" />
        <button
          className="linklike"
          type="button"
          onClick={async () => {
            await logout();
            navigate("/cabinet/login", { replace: true });
          }}
        >
          Выйти
        </button>
      </aside>

      <div className="cabinet-main">
        <header className="cabinet-header">
          <div>
            <b>{me.fullName}</b>
            <div className="muted small">
              {me.role === "ADMIN" ? "Администратор" : "Врач"}
              <span className="header-email"> · {me.email}</span>
            </div>
          </div>
          <div className="row messenger-actions">
            <span className={`badge ${me.role === "ADMIN" ? "warn" : "neutral"}`}>
              {me.role === "ADMIN" ? "Админ" : "Врач"}
            </span>
            {linked ? (
              <>
                <span className="badge neutral">Уведомления: {linkedLabel}</span>
                <button className="btn btn-sm" disabled={busy} onClick={() => void unlinkMessenger()}>
                  Отвязать
                </button>
              </>
            ) : (
              connectButtons
            )}
          </div>
        </header>

        <main className="cabinet-content">
          {error ? <ErrorBox message={error} /> : null}
          <Outlet />
        </main>
      </div>

      <nav className="mobile-nav" aria-label="Разделы кабинета">
        <NavLink to="/cabinet" end className={({ isActive }) => (isActive ? "active" : "")}>
          Обзор
        </NavLink>
        <NavLink
          to="/cabinet/patients"
          className={({ isActive }) => (isActive ? "active" : "")}
        >
          Пациентки
        </NavLink>
        <NavLink
          to="/cabinet/calendar"
          className={({ isActive }) => (isActive ? "active" : "")}
        >
          Календарь
        </NavLink>
        <NavLink
          to="/cabinet/alerts"
          className={({ isActive }) => (isActive ? "active" : "")}
        >
          События
        </NavLink>
        <button type="button" onClick={() => setMoreOpen(true)}>
          Ещё
        </button>
      </nav>

      {moreOpen ? (
        <div className="modal-backdrop" onClick={() => setMoreOpen(false)}>
          <div className="modal mobile-sheet" onClick={(event) => event.stopPropagation()}>
            <h2>Ещё</h2>
            <div className="mobile-sheet-list mt">
              <Link to="/cabinet/analytics" onClick={() => setMoreOpen(false)}>
                Аналитика
              </Link>
              <Link to="/cabinet/help" onClick={() => setMoreOpen(false)}>
                Обучение
              </Link>
              {me.role === "ADMIN" ? (
                <Link to="/cabinet/admin" onClick={() => setMoreOpen(false)}>
                  Врачи
                </Link>
              ) : null}
              {me.role === "ADMIN" ? (
                <Link to="/cabinet/audit" onClick={() => setMoreOpen(false)}>
                  Аудит
                </Link>
              ) : null}
              {linked ? (
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => {
                    setMoreOpen(false);
                    void unlinkMessenger();
                  }}
                >
                  Отвязать уведомления ({linkedLabel})
                </button>
              ) : (
                <>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => {
                      setMoreOpen(false);
                      void connectMessenger("TELEGRAM");
                    }}
                  >
                    Подключить Telegram
                  </button>
                  <button
                    type="button"
                    disabled={busy}
                    onClick={() => {
                      setMoreOpen(false);
                      void connectMessenger("MAX");
                    }}
                  >
                    Подключить MAX
                  </button>
                </>
              )}
              <button
                type="button"
                className="danger"
                onClick={async () => {
                  setMoreOpen(false);
                  await logout();
                  navigate("/cabinet/login", { replace: true });
                }}
              >
                Выйти
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {messenger ? (
        <div className="modal-backdrop" onClick={closeMessenger}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <h2>Уведомления в {PLATFORM_LABEL[messengerPlatform]}</h2>
            <p className="muted mt">
              {messenger.linked
                ? "Уведомления уже подключены. Новая ссылка заменит текущую привязку после подтверждения в боте."
                : "Откройте ссылку на телефоне и нажмите Start в боте."}
            </p>
            <div className="card tight mt" style={{ marginBottom: 0 }}>
              <span className="mono small" style={{ wordBreak: "break-all" }}>
                {messenger.url}
              </span>
            </div>
            <div className="row mt">
              <a className="btn btn-primary" href={messenger.url} target="_blank" rel="noreferrer">
                Открыть {PLATFORM_LABEL[messengerPlatform]}
              </a>
              <button
                className="btn"
                onClick={() => void navigator.clipboard?.writeText(messenger.url)}
              >
                Скопировать
              </button>
              <button className="btn btn-ghost" onClick={closeMessenger}>
                Закрыть
              </button>
            </div>
            <p className="notice mt">Ссылка действует 30 минут и один раз.</p>
          </div>
        </div>
      ) : null}
    </div>
  );
}
