import { Navigate } from "react-router-dom";
import { MiniShell } from "../components/MiniShell";
import { ErrorBox, Spinner } from "../components/ui";
import { usePatient } from "../session";

export default function ConnectPage() {
  const { me, loading, error, refresh } = usePatient();

  if (loading) {
    return (
      <MiniShell subtitle="Подключение">
        <Spinner />
      </MiniShell>
    );
  }
  if (error) {
    return (
      <MiniShell subtitle="Подключение">
        <ErrorBox message={error} onRetry={() => void refresh()} />
      </MiniShell>
    );
  }
  if (me?.bound) {
    return <Navigate to="/app" replace />;
  }

  return (
    <MiniShell subtitle="Подключение к врачу">
      <div className="card">
        <h1 style={{ fontSize: 22, marginBottom: 10 }}>Нужен QR-код врача</h1>
        <p className="muted" style={{ marginBottom: 14 }}>
          Врач создаёт вашу карточку в кабинете и показывает QR-код. Отсканируйте его камерой
          телефона — откроется этот бот, и вы подтвердите привязку.
        </p>
        <ol className="muted" style={{ paddingLeft: 20, lineHeight: 1.7 }}>
          <li>Получите QR-код у своего врача.</li>
          <li>Отсканируйте его камерой или в Telegram.</li>
          <li>Проверьте ФИО, возраст и ПДР, поставьте согласие.</li>
          <li>Заполните рост и вес до беременности.</li>
        </ol>
        <button className="btn btn-primary btn-block mt" type="button" onClick={() => void refresh()}>
          Проверить привязку
        </button>
      </div>
      <div className="card tight">
        <p className="notice">
          Приложение не заменяет врача. При тревожных симптомах звоните 103 или 112.
        </p>
      </div>
    </MiniShell>
  );
}
