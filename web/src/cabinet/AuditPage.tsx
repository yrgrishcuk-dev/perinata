import { useEffect, useState } from "react";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDateTime } from "../format";
import { useStaff } from "../session";
import type { AuditLog } from "../types";

export default function AuditPage() {
  const { me, token } = useStaff();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token || me?.role !== "ADMIN") {
      setLoading(false);
      return;
    }
    request<AuditLog[]>("/api/admin/audit", { token })
      .then((data) => {
        setLogs(data);
        setError(null);
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Не удалось загрузить аудит"))
      .finally(() => setLoading(false));
  }, [token, me?.role]);

  if (me?.role !== "ADMIN") {
    return (
      <div className="card">
        <h2>Недоступно</h2>
        <p className="muted mt">Раздел доступен только администратору клиники.</p>
      </div>
    );
  }
  if (loading) {
    return <Spinner />;
  }
  if (error) {
    return <ErrorBox message={error} />;
  }

  return (
    <>
      <h1 style={{ fontSize: 24, marginBottom: 16 }}>Аудит</h1>
      <div className="card">
        {logs.length === 0 ? (
          <p className="muted">Записей нет.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Время</th>
                  <th>Кто</th>
                  <th>Действие</th>
                  <th>Объект</th>
                  <th>IP</th>
                  <th>Детали</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDateTime(log.createdAt)}</td>
                    <td>
                      {log.actorType}
                      {log.actorId ? ` #${log.actorId}` : ""}
                    </td>
                    <td>{log.action}</td>
                    <td>
                      {log.entityType ?? "—"}
                      {log.entityId ? ` #${log.entityId}` : ""}
                    </td>
                    <td className="mono small">{log.ip ?? "—"}</td>
                    <td className="mono small">{log.metadata ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}
