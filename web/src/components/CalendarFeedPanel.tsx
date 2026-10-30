import { useEffect, useState } from "react";
import { ErrorBox } from "./ui";
import { request } from "../api";
import type { CalendarFeedLink, CalendarFeedStatus } from "../types";

interface CalendarFeedPanelProps {
  apiPath: string;
  token: string | null;
  title?: string;
}

export function CalendarFeedPanel({
  apiPath,
  token,
  title = "Подписка на календарь"
}: CalendarFeedPanelProps) {
  const [status, setStatus] = useState<CalendarFeedStatus | null>(null);
  const [link, setLink] = useState<CalendarFeedLink | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!token) {
      return;
    }
    request<CalendarFeedStatus>(apiPath, { token })
      .then(setStatus)
      .catch(() => setStatus({ active: false, createdAt: null }));
  }, [apiPath, token]);

  const create = async () => {
    if (!token) {
      return;
    }
    setBusy(true);
    setError(null);
    setCopied(false);
    try {
      const created = await request<CalendarFeedLink>(apiPath, {
        method: "POST",
        token
      });
      setLink(created);
      setStatus({ active: true, createdAt: created.createdAt });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось создать ссылку");
    } finally {
      setBusy(false);
    }
  };

  const revoke = async () => {
    if (!token) {
      return;
    }
    if (!window.confirm("Отозвать ссылку подписки? Она сразу перестанет работать.")) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await request(apiPath, { method: "DELETE", token });
      setLink(null);
      setStatus({ active: false, createdAt: null });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось отозвать ссылку");
    } finally {
      setBusy(false);
    }
  };

  const copy = async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
    } catch {
      setError("Не удалось скопировать. Скопируйте ссылку вручную.");
    }
  };

  return (
    <div className="card calendar-feed-panel">
      <div className="row between">
        <h2>{title}</h2>
        {status?.active ? <span className="badge ok">Активна</span> : null}
      </div>
      <p className="muted mt">
        Приёмы будут автоматически появляться в Google, Apple или Outlook Calendar. Личные
        события и медицинские данные в подписку не попадают.
      </p>

      {link ? (
        <div className="calendar-feed-link mt">
          <label className="field">
            <span>Ссылка для Google/Outlook</span>
            <input readOnly value={link.url} onFocus={(event) => event.target.select()} />
          </label>
          <div className="row">
            <button className="btn btn-sm" type="button" onClick={() => void copy(link.url)}>
              {copied ? "Скопировано" : "Скопировать"}
            </button>
            <a className="btn btn-sm" href={link.webcalUrl}>
              Открыть в Apple Calendar
            </a>
          </div>
          <p className="notice">
            Ссылка показывается один раз. Сохраните её; если потеряете — перевыпустите.
          </p>
        </div>
      ) : (
        <p className="muted mt">
          {status?.active
            ? "Ссылка активна. Если вы её потеряли, перевыпустите — старая перестанет работать."
            : "Ссылка ещё не создана."}
        </p>
      )}

      {error ? <ErrorBox message={error} /> : null}

      <div className="row mt">
        <button
          className="btn btn-primary"
          type="button"
          disabled={busy}
          onClick={() => void create()}
        >
          {busy ? "Создаём…" : status?.active ? "Перевыпустить" : "Создать ссылку"}
        </button>
        {status?.active ? (
          <button
            className="btn btn-soft-danger"
            type="button"
            disabled={busy}
            onClick={() => void revoke()}
          >
            Отозвать
          </button>
        ) : null}
      </div>
    </div>
  );
}
