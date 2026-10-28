import type { ReactNode } from "react";
import type { AlarmStatus, RiskGroup, Severity } from "../types";

export function Spinner() {
  return <div className="spinner" aria-label="Загрузка" />;
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="alert danger">
      <div>{message}</div>
      {onRetry ? (
        <button className="btn btn-sm mt" type="button" onClick={onRetry}>
          Повторить
        </button>
      ) : null}
    </div>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="muted">{children}</div>;
}

export function SeverityBadge({ severity }: { severity: Severity }) {
  const cls = severity === "CRITICAL" ? "danger" : severity === "WARNING" ? "warn" : "neutral";
  const label =
    severity === "CRITICAL" ? "Тревога" : severity === "WARNING" ? "Внимание" : "Инфо";
  return <span className={`badge ${cls}`}>{label}</span>;
}

export function AlarmStatusBadge({ status }: { status: AlarmStatus }) {
  const cls = status === "NEW" ? "danger" : status === "ACKED" ? "warn" : "neutral";
  const label = status === "NEW" ? "Новое" : status === "ACKED" ? "В работе" : "Закрыто";
  return <span className={`badge ${cls}`}>{label}</span>;
}

export function RiskBadge({ risk }: { risk: RiskGroup }) {
  return risk === "HIGH" ? (
    <span className="badge danger">Высокий риск</span>
  ) : (
    <span className="badge ok">Низкий риск</span>
  );
}

export function Field({
  label,
  hint,
  children
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint ? <span className="notice">{hint}</span> : null}
    </label>
  );
}
