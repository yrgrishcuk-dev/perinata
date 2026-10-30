import { useEffect, useState } from "react";
import { Link, Navigate, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { useStaff } from "../session";
import type { MessengerLinkResponse } from "../types";

export default function CabinetLayout() {
  const { me, loading, logout, token } = useStaff();
  const navigate = useNavigate();
  const location = useLocation();
  const [messenger, setMessenger] = useState<MessengerLinkResponse | null>(null);
  const [messengerPlatform, setMessengerPlatform] = useState<"TELEGRAM" | "MAX">("TELEGRAM");
  const [error, setError] = useState<string | null>(null);
  const [moreOpen, setMoreOpen] = useState(false);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);

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

  const connectMessenger = async (platform: "TELEGRAM" | "MAX") => {
    if (!token) {
      return;
    }
    setMessengerPlatform(platform);
    try {
      const data = await request<MessengerLinkResponse>(
        `/api/doctor/messenger-link?platform=${platform}`,
        { method: "POST", token }
      );
      setMessenger(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось создать ссылку");
    }
  };

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
          <div className="row">
            <span className={`badge ${me.role === "ADMIN" ? "warn" : "neutral"}`}>
              {me.role === "ADMIN" ? "Админ" : "Врач"}
            </span>
            <button className="btn btn-sm" onClick={() => void connectMessenger("TELEGRAM")}>
              Telegram
            </button>
            <button className="btn btn-sm" onClick={() => void connectMessenger("MAX")}>
              MAX
            </button>
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
              <button
                type="button"
                onClick={() => {
                  setMoreOpen(false);
                  void connectMessenger("TELEGRAM");
                }}
              >
                Уведомления в Telegram
              </button>
              <button
                type="button"
                onClick={() => {
                  setMoreOpen(false);
                  void connectMessenger("MAX");
                }}
              >
                Уведомления в MAX
              </button>
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
        <div className="modal-backdrop" onClick={() => setMessenger(null)}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <h2>Уведомления в {messengerPlatform === "MAX" ? "MAX" : "Telegram"}</h2>
            <p className="muted mt">
              {messenger.linked
                ? "Уведомления уже подключены. Новая ссылка заменит текущую привязку."
                : "Откройте ссылку на телефоне и нажмите Start в боте."}
            </p>
            <div className="card tight mt" style={{ marginBottom: 0 }}>
              <span className="mono small" style={{ wordBreak: "break-all" }}>
                {messenger.url}
              </span>
            </div>
            <div className="row mt">
              <a className="btn btn-primary" href={messenger.url} target="_blank" rel="noreferrer">
                Открыть {messengerPlatform === "MAX" ? "MAX" : "Telegram"}
              </a>
              <button
                className="btn"
                onClick={() => void navigator.clipboard?.writeText(messenger.url)}
              >
                Скопировать
              </button>
              <button className="btn btn-ghost" onClick={() => setMessenger(null)}>
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
