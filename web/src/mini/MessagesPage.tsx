import { useCallback, useEffect, useState } from "react";
import { ErrorBox, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDateTime } from "../format";
import { usePatient } from "../session";
import type { MessagesResponse } from "../types";

export default function MessagesPage() {
  const { token, refresh } = usePatient();
  const [data, setData] = useState<MessagesResponse | null>(null);
  const [body, setBody] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    setLoading(true);
    try {
      const response = await request<MessagesResponse>("/api/patient/messages", { token });
      setData(response);
      setError(null);
      await request<void>("/api/patient/messages/read", { method: "POST", token });
      await refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить сообщения");
    } finally {
      setLoading(false);
    }
  }, [token, refresh]);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading && !data) {
    return <Spinner />;
  }
  if (error && !data) {
    return <ErrorBox message={error} onRetry={() => void load()} />;
  }

  const send = async () => {
    if (!token || !body.trim()) {
      return;
    }
    setBusy(true);
    try {
      const response = await request<MessagesResponse>("/api/patient/messages", {
        method: "POST",
        body: { body: body.trim() },
        token
      });
      setData(response);
      setBody("");
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось отправить сообщение");
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <div className="card">
        <h1 style={{ fontSize: 21, marginBottom: 6 }}>Сообщения врачу</h1>
        <p className="muted">
          Это не экстренная связь. При тревожных симптомах звоните 103 или 112.
        </p>
      </div>

      <div className="card">
        {!data || data.messages.length === 0 ? (
          <p className="muted">Сообщений пока нет. Напишите врачу, если есть вопрос.</p>
        ) : (
          <div className="thread">
            {data.messages.map((message) => {
              const mine = message.direction === "TO_DOCTOR";
              return (
                <div className={`bubble-row ${mine ? "mine" : "theirs"}`} key={message.id}>
                  <div className={`bubble ${mine ? "mine" : "theirs"}`}>
                    <p>{message.body}</p>
                    <span className="small">{formatDateTime(message.createdAt)}</span>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {error ? <ErrorBox message={error} /> : null}

        <div className="row mt">
          <input
            value={body}
            onChange={(event) => setBody(event.target.value)}
            placeholder="Ваше сообщение"
            maxLength={2000}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                void send();
              }
            }}
          />
          <button className="btn btn-primary" disabled={busy || !body.trim()} onClick={() => void send()}>
            Отправить
          </button>
        </div>
      </div>
    </>
  );
}
