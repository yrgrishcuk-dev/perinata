import { NavLink, Navigate, Outlet } from "react-router-dom";
import { usePatient } from "../session";
import { ErrorBox, Spinner } from "../components/ui";
import { formatDate } from "../format";

const nav = [
  { to: "/app", label: "Главная", end: true },
  { to: "/app/diary", label: "Дневник" },
  { to: "/app/symptoms", label: "Симптомы" },
  { to: "/app/contractions", label: "Схватки" },
  { to: "/app/week", label: "Неделя" }
];

export default function MiniLayout() {
  const { me, loading, error, refresh } = usePatient();

  if (loading) {
    return (
      <div className="shell">
        <Spinner />
      </div>
    );
  }
  if (error) {
    return (
      <div className="shell">
        <main className="content narrow">
          <ErrorBox message={error} onRetry={() => void refresh()} />
        </main>
      </div>
    );
  }
  if (!me?.bound || !me.home) {
    return <Navigate to="/connect" replace />;
  }

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          Антенатальный помощник
          <small>
            {me.home.summary.gestationalAge} · ПДР {formatDate(me.home.summary.edd)}
          </small>
        </div>
        <span className="badge mono">{me.home.summary.publicCode}</span>
      </header>
      <main className="content">
        <Outlet />
      </main>
      <nav className="bottomnav">
        {nav.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => (isActive ? "active" : "")}
          >
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
