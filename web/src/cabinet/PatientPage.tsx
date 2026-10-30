import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import QRCode from "qrcode";
import { GainChart } from "../components/GainChart";
import { Counter } from "../components/Counter";
import { ActionMenu } from "../components/ActionMenu";
import {
  AlarmStatusBadge,
  ErrorBox,
  Field,
  RiskBadge,
  SeverityBadge,
  Spinner
} from "../components/ui";
import { request } from "../api";
import {
  alarmTypeLabel,
  formatDate,
  formatDateTime,
  formatPayload,
  gainStatusClass,
  gainStatusLabel
} from "../format";
import { useStaff } from "../session";
import { toIsoDate } from "../lib/calendar";
import type {
  AlertView,
  DiabetesType,
  DiaryView,
  LinkResponse,
  MessagesResponse,
  PatientDetails,
  PatientUpdateInput,
  RiskGroup
} from "../types";

type Tab = "overview" | "diary" | "alerts" | "messages" | "settings";

interface SettingsState {
  fullName: string;
  ageYears: string;
  parity: string;
  edd: string;
  heightCm: string;
  prePregnancyWeightKg: string;
  riskGroup: RiskGroup;
  diabetesType: DiabetesType;
  diaryRequired: boolean;
  reminderTime: string;
  comment: string;
}

const thresholdLabels: Record<string, string> = {
  SBP_WARN: "Верхнее АД, внимание",
  SBP_CRIT: "Верхнее АД, критично",
  DBP_WARN: "Нижнее АД, внимание",
  DBP_CRIT: "Нижнее АД, критично",
  GLU_FAST_WARN: "Глюкоза натощак, внимание",
  GLU_FAST_CRIT: "Глюкоза натощак, критично",
  GLU_POST_WARN: "Глюкоза после еды, внимание",
  GLU_POST_CRIT: "Глюкоза после еды, критично",
  WEIGHT_GAIN_WARN: "Прибавка веса, внимание"
};

export default function PatientPage() {
  const { id } = useParams();
  const patientId = Number(id);
  const navigate = useNavigate();
  const { token } = useStaff();

  const [details, setDetails] = useState<PatientDetails | null>(null);
  const [tab, setTab] = useState<Tab>("overview");
  const tabsRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const changeTab = (next: Tab) => {
    setTab(next);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  useEffect(() => {
    tabsRef.current
      ?.querySelector<HTMLButtonElement>("button.active")
      ?.scrollIntoView({ behavior: "smooth", inline: "center", block: "nearest" });
  }, [tab]);

  const [diary, setDiary] = useState<DiaryView[]>([]);
  const [alerts, setAlerts] = useState<AlertView[]>([]);
  const [messages, setMessages] = useState<MessagesResponse | null>(null);
  const [messageBody, setMessageBody] = useState("");

  const [settings, setSettings] = useState<SettingsState | null>(null);
  const [thresholds, setThresholds] = useState<Record<string, string>>({});
  const [savingSettings, setSavingSettings] = useState(false);
  const [savingThresholds, setSavingThresholds] = useState(false);

  const [link, setLink] = useState<LinkResponse | null>(null);
  const [qr, setQr] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const loadDetails = useCallback(async () => {
    if (!token || !Number.isFinite(patientId)) {
      return;
    }
    try {
      const data = await request<PatientDetails>(`/api/doctor/patients/${patientId}`, { token });
      setDetails(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить карточку");
    } finally {
      setLoading(false);
    }
  }, [token, patientId]);

  useEffect(() => {
    void loadDetails();
  }, [loadDetails]);

  useEffect(() => {
    if (!details) {
      return;
    }
    setSettings({
      fullName: details.summary.fullName,
      ageYears: String(details.summary.ageYears),
      parity: details.summary.parity != null ? String(details.summary.parity) : "",
      edd: details.summary.edd,
      heightCm: details.heightCm != null ? String(details.heightCm) : "",
      prePregnancyWeightKg:
        details.prePregnancyWeightKg != null ? String(details.prePregnancyWeightKg) : "",
      riskGroup: details.summary.riskGroup,
      diabetesType: details.summary.diabetesType,
      diaryRequired: details.summary.diaryRequired,
      reminderTime: details.reminderTime ? details.reminderTime.slice(0, 5) : "09:00",
      comment: details.comment ?? ""
    });
    setThresholds(
      Object.fromEntries(Object.entries(details.thresholds).map(([key, value]) => [key, String(value)]))
    );
  }, [details]);

  useEffect(() => {
    if (!token || !details) {
      return;
    }
    if (tab === "diary") {
      request<DiaryView[]>(`/api/doctor/patients/${patientId}/diary`, { token })
        .then(setDiary)
        .catch((e: unknown) => setActionError(e instanceof Error ? e.message : "Ошибка"));
    }
    if (tab === "alerts") {
      request<AlertView[]>(`/api/doctor/patients/${patientId}/alerts`, { token })
        .then(setAlerts)
        .catch((e: unknown) => setActionError(e instanceof Error ? e.message : "Ошибка"));
    }
    if (tab === "messages") {
      request<MessagesResponse>(`/api/doctor/patients/${patientId}/messages`, { token })
        .then(async (data) => {
          setMessages(data);
          await request<void>(`/api/doctor/patients/${patientId}/messages/read`, {
            method: "POST",
            token
          });
        })
        .catch((e: unknown) => setActionError(e instanceof Error ? e.message : "Ошибка"));
    }
  }, [tab, token, details, patientId]);

  if (loading) {
    return <Spinner />;
  }
  if (error || !details) {
    return <ErrorBox message={error ?? "Карточка не найдена"} />;
  }

  const summary = details.summary;
  const gain = details.gain;

  const createLink = async () => {
    if (!token) {
      return;
    }
    setActionError(null);
    try {
      const data = await request<LinkResponse>(`/api/doctor/patients/${patientId}/link`, {
        method: "POST",
        token
      });
      setLink(data);
      setQr(
        await QRCode.toDataURL(data.url, {
          width: 320,
          margin: 1,
          color: { dark: "#0f172a", light: "#ffffff" }
        })
      );
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось создать QR");
    }
  };

  const revoke = async () => {
    if (!token || !window.confirm("Отвязать мессенджер от карточки? Пациентке понадобится новый QR.")) {
      return;
    }
    try {
      await request<void>(`/api/doctor/patients/${patientId}/revoke-binding`, {
        method: "POST",
        token
      });
      setNotice("Привязка отозвана");
      await loadDetails();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось отвязать");
    }
  };

  const deleteCard = async () => {
    if (
      !token ||
      !window.confirm(
        "Удалить карточку и все данные пациентки? Дневник, алерты, сообщения и привязка будут удалены без возможности восстановления."
      )
    ) {
      return;
    }
    try {
      await request<void>(`/api/doctor/patients/${patientId}`, {
        method: "DELETE",
        token
      });
      navigate("/cabinet/patients", { replace: true });
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось удалить пациентку");
    }
  };

  const closeCard = async () => {
    if (!token || !window.confirm("Закрыть наблюдение? Пациентка потеряет доступ в приложении.")) {
      return;
    }
    try {
      await request<void>(`/api/doctor/patients/${patientId}/close`, {
        method: "POST",
        body: { comment: "Закрыто врачом" },
        token
      });
      setNotice("Наблюдение закрыто");
      await loadDetails();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось закрыть");
    }
  };

  const acknowledge = async (alertId: number, close = false) => {
    if (!token) {
      return;
    }
    const comment = window.prompt("Комментарий (необязательно)");
    if (comment === null) {
      return;
    }
    try {
      await request<AlertView>(
        `/api/doctor/alerts/${alertId}/${close ? "close" : "ack"}`,
        { method: "POST", body: { comment }, token }
      );
      setAlerts(
        await request<AlertView[]>(`/api/doctor/patients/${patientId}/alerts`, { token })
      );
      await loadDetails();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось обновить событие");
    }
  };

  const saveSettings = async () => {
    if (!token || !settings) {
      return;
    }
    setSavingSettings(true);
    setActionError(null);
    try {
      const body: PatientUpdateInput = {
        fullName: settings.fullName,
        ageYears: Number(settings.ageYears),
        parity: Number(settings.parity || "1"),
        edd: settings.edd,
        heightCm: settings.heightCm ? Number(settings.heightCm.replace(",", ".")) : null,
        prePregnancyWeightKg: settings.prePregnancyWeightKg
          ? Number(settings.prePregnancyWeightKg.replace(",", "."))
          : null,
        riskGroup: settings.riskGroup,
        diabetesType: settings.diabetesType,
        diaryRequired: settings.diaryRequired,
        reminderTime: settings.reminderTime,
        comment: settings.comment.trim()
      };
      await request(`/api/doctor/patients/${patientId}`, {
        method: "PATCH",
        body,
        token
      });
      setNotice("Карточка обновлена");
      await loadDetails();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось сохранить");
    } finally {
      setSavingSettings(false);
    }
  };

  const saveThresholds = async () => {
    if (!token) {
      return;
    }
    setSavingThresholds(true);
    setActionError(null);
    try {
      const values = Object.fromEntries(
        Object.entries(thresholds)
          .filter(([, value]) => value.trim() !== "")
          .map(([key, value]) => [key, Number(value.replace(",", "."))])
      );
      await request(`/api/doctor/patients/${patientId}/thresholds`, {
        method: "PUT",
        body: { thresholds: values },
        token
      });
      setNotice("Пороги обновлены");
      await loadDetails();
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось сохранить пороги");
    } finally {
      setSavingThresholds(false);
    }
  };

  const sendMessage = async () => {
    if (!token || !messageBody.trim()) {
      return;
    }
    try {
      const data = await request<MessagesResponse>(
        `/api/doctor/patients/${patientId}/messages`,
        { method: "POST", body: { body: messageBody.trim() }, token }
      );
      setMessages(data);
      setMessageBody("");
    } catch (e) {
      setActionError(e instanceof Error ? e.message : "Не удалось отправить");
    }
  };

  return (
    <>
      <div className="patient-sticky">
        <div className="page-head row between mb patient-head">
          <div>
            <Link className="muted small" to="/cabinet/patients">
              ← Пациентки
            </Link>
            <h1 style={{ fontSize: 24, marginTop: 6 }}>{summary.fullName}</h1>
            <div className="row">
              <span className="badge mono">{summary.publicCode}</span>
              <RiskBadge risk={summary.riskGroup} />
              {summary.bound ? (
                <span className="badge ok">Привязана</span>
              ) : (
                <span className="badge neutral">Не привязана</span>
              )}
              {summary.status === "CLOSED" ? (
                <span className="badge neutral">Закрыта</span>
              ) : null}
            </div>
          </div>
          <div className="row desktop-actions">
            <button
              className="btn"
              onClick={() =>
                navigate(
                  `/cabinet/calendar?appointment=1&patientId=${patientId}&date=${toIsoDate(new Date())}`
                )
              }
            >
              Записать на приём
            </button>
            <button
              className="btn"
              onClick={() =>
                navigate(
                  `/cabinet/calendar?new=1&patientId=${patientId}&date=${toIsoDate(new Date())}`
                )
              }
            >
              Добавить событие
            </button>
            <button className="btn btn-primary" onClick={() => void createLink()}>
              QR для привязки
            </button>
          </div>
          <div className="mobile-actions row">
            <button
              className="btn btn-primary btn-sm"
              type="button"
              onClick={() =>
                navigate(
                  `/cabinet/calendar?appointment=1&patientId=${patientId}&date=${toIsoDate(new Date())}`
                )
              }
            >
              Приём
            </button>
            <ActionMenu>
              <button
                type="button"
                onClick={() =>
                  navigate(
                    `/cabinet/calendar?new=1&patientId=${patientId}&date=${toIsoDate(new Date())}`
                  )
                }
              >
                Добавить событие
              </button>
              <button type="button" onClick={() => void createLink()}>
                QR для привязки
              </button>
              <button type="button" onClick={() => changeTab("settings")}>
                Настройки
              </button>
            </ActionMenu>
          </div>
        </div>

        <div className="tabs" ref={tabsRef}>
          {(
            [
              ["overview", "Обзор"],
              ["diary", "Дневник"],
              ["alerts", "События"],
              ["messages", "Сообщения"],
              ["settings", "Настройки"]
            ] as [Tab, string][]
          ).map(([key, label]) => (
            <button
              key={key}
              className={tab === key ? "active" : ""}
              onClick={() => changeTab(key)}
              type="button"
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {notice ? <div className="alert ok">{notice}</div> : null}
      {actionError ? <ErrorBox message={actionError} /> : null}

      {tab === "overview" ? (
        <>
          {details.heightCm == null || details.prePregnancyWeightKg == null ? (
            <div className="alert info">
              <div>
                Рост и вес до беременности не заполнены — ИМТ и коридор прибавки не
                рассчитываются.
              </div>
              <button className="btn btn-sm mt" type="button" onClick={() => changeTab("settings")}>
                Заполнить
              </button>
            </div>
          ) : null}

          <div className="grid three mb">
            <div className="stat">
              <span className="muted small">Срок</span>
              <b style={{ fontSize: 18 }}>{summary.gestationalAge}</b>
            </div>
            <div className="stat">
              <span className="muted small">ПДР</span>
              <b style={{ fontSize: 18 }}>{formatDate(summary.edd)}</b>
            </div>
            <div className="stat">
              <span className="muted small">ИМТ до беременности</span>
              <b style={{ fontSize: 18 }}>
                {summary.bmi ?? "—"} {summary.bmiCategoryLabel ? `· ${summary.bmiCategoryLabel}` : ""}
              </b>
            </div>
          </div>

          <div className="card">
            <div className="row between">
              <h2>Прибавка веса</h2>
              <span className={`badge ${gainStatusClass(gain.status)}`}>
                {gainStatusLabel(gain.status)}
              </span>
            </div>
            <p className="muted" style={{ marginTop: 6 }}>
              {gain.gainKg != null ? `${gain.gainKg.toFixed(1)} кг` : "Нет данных"}
              {gain.minGainKg != null && gain.maxGainKg != null
                ? ` · коридор ${gain.minGainKg.toFixed(1)}–${gain.maxGainKg.toFixed(1)} кг`
                : ""}
            </p>
            <GainChart gain={gain} />
          </div>

          <div className="card">
            <h2>Карточка</h2>
            <dl className="kv mt">
              <dt>Врач</dt>
              <dd>{details.doctorName ?? "—"}</dd>
              <dt>Возраст</dt>
              <dd>{summary.ageYears}</dd>
              <dt>Роды</dt>
              <dd>{summary.parity ?? "—"}</dd>
              <dt>Группа риска</dt>
              <dd>{summary.riskGroup === "HIGH" ? "Высокий" : "Низкий"}</dd>
              <dt>Диабет</dt>
              <dd>
                {summary.diabetesType === "NONE"
                  ? "Нет"
                  : summary.diabetesType === "GESTATIONAL"
                    ? "Гестационный"
                    : "До беременности"}
              </dd>
              <dt>Дневник обязателен</dt>
              <dd>{summary.diaryRequired ? "Да" : "Нет"}</dd>
              <dt>Комментарий</dt>
              <dd>{details.comment ?? "—"}</dd>
              <dt>Привязка</dt>
              <dd>{details.boundPlatform ?? "нет"}</dd>
              <dt>Согласие</dt>
              <dd>{details.consentVersion ?? "—"}</dd>
              <dt>Дано</dt>
              <dd>{formatDateTime(details.consentAt)}</dd>
              <dt>Уведомления врачу</dt>
              <dd>{details.hasMessengerNotifications ? "Подключены" : "Не подключены"}</dd>
            </dl>
            <div className="row mt">
              <button className="btn btn-sm" onClick={() => void revoke()} disabled={!summary.bound}>
                Отвязать мессенджер
              </button>
              <button className="btn btn-soft-danger btn-sm" onClick={() => void closeCard()}>
                Закрыть наблюдение
              </button>
            </div>
          </div>
        </>
      ) : null}

      {tab === "diary" ? (
        <div className="card">
          <h2>Дневник</h2>
          {diary.length === 0 ? (
            <p className="muted mt">Записей пока нет.</p>
          ) : (
            <div className="table-wrap mt">
              <table>
                <thead>
                  <tr>
                    <th>Дата</th>
                    <th>АД</th>
                    <th>Пульс</th>
                    <th>Вес</th>
                    <th>Глюкоза</th>
                    <th>Заметка</th>
                  </tr>
                </thead>
                <tbody>
                  {diary
                    .slice()
                    .reverse()
                    .map((entry) => (
                      <tr key={entry.id}>
                        <td>{formatDate(entry.date)}</td>
                        <td>{entry.sbp != null || entry.dbp != null ? `${entry.sbp ?? "—"}/${entry.dbp ?? "—"}` : "—"}</td>
                        <td>{entry.pulse ?? "—"}</td>
                        <td>{entry.weightKg ?? "—"}</td>
                        <td>
                          {entry.glucoseMmol != null
                            ? `${entry.glucoseMmol} ${
                                entry.glucoseContext === "POSTPRANDIAL" ? "после еды" : "натощак"
                              }`
                            : "—"}
                        </td>
                        <td>{entry.note ?? "—"}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : null}

      {tab === "alerts" ? (
        <div className="card">
          <h2>События</h2>
          {alerts.length === 0 ? (
            <p className="muted mt">Событий нет.</p>
          ) : (
            <div className="list mt">
              {alerts.map((alert) => (
                <div className="list-item" key={alert.id}>
                  <div className="row between">
                    <div className="row">
                      <SeverityBadge severity={alert.severity} />
                      <b>{alarmTypeLabel(alert.type)}</b>
                      <AlarmStatusBadge status={alert.status} />
                    </div>
                    <span className="muted small">{formatDateTime(alert.createdAt)}</span>
                  </div>
                  {alert.payload ? (
                    <p className="muted small mt mono">{formatPayload(alert.payload)}</p>
                  ) : null}
                  {alert.comment ? <p className="muted small mt">Комментарий: {alert.comment}</p> : null}
                  {alert.status !== "CLOSED" ? (
                    <div className="row mt">
                      <button className="btn btn-sm" onClick={() => void acknowledge(alert.id)}>
                        Взять в работу
                      </button>
                      <button className="btn btn-sm" onClick={() => void acknowledge(alert.id, true)}>
                        Закрыть
                      </button>
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          )}
        </div>
      ) : null}

      {tab === "messages" ? (
        <div className="card">
          <h2>Сообщения</h2>
          {!messages || messages.messages.length === 0 ? (
            <p className="muted mt">Сообщений пока нет.</p>
          ) : (
            <div className="thread mt">
              {messages.messages.map((message) => {
                const mine = message.direction === "TO_PATIENT";
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
          <div className="row mt">
            <input
              value={messageBody}
              onChange={(event) => setMessageBody(event.target.value)}
              placeholder="Сообщение пациентке"
              onKeyDown={(event) => {
                if (event.key === "Enter" && !event.shiftKey) {
                  event.preventDefault();
                  void sendMessage();
                }
              }}
            />
            <button className="btn btn-primary" onClick={() => void sendMessage()}>
              Отправить
            </button>
          </div>
          <p className="notice mt">
            Сообщение не заменяет экстренную помощь. При тревожных симптомах пациентка должна звонить
            103.
          </p>
        </div>
      ) : null}

      {tab === "settings" && settings ? (
        <>
          <div className="card">
            <h2>Данные карточки</h2>
            <div className="grid two mt">
              <Field label="ФИО" hint="Эти данные отобразятся у пациентки в карточке.">
                <input
                  value={settings.fullName}
                  onChange={(event) => setSettings({ ...settings, fullName: event.target.value })}
                />
              </Field>
              <Field label="Возраст">
                <input
                  inputMode="numeric"
                  value={settings.ageYears}
                  onChange={(event) => setSettings({ ...settings, ageYears: event.target.value })}
                />
              </Field>
              <Field label="Роды" hint="Какими по счёту будут эти роды.">
                <Counter
                  value={settings.parity || "1"}
                  onChange={(parity) => setSettings({ ...settings, parity })}
                />
              </Field>
              <Field label="ПДР">
                <input
                  type="date"
                  value={settings.edd}
                  onChange={(event) => setSettings({ ...settings, edd: event.target.value })}
                />
              </Field>
              <Field label="Напоминание о дневнике">
                <input
                  type="time"
                  value={settings.reminderTime}
                  onChange={(event) =>
                    setSettings({ ...settings, reminderTime: event.target.value })
                  }
                />
              </Field>
              <Field label="Рост, см" hint="Заполняется только врачом.">
                <input
                  inputMode="decimal"
                  value={settings.heightCm}
                  onChange={(event) => setSettings({ ...settings, heightCm: event.target.value })}
                  placeholder="см"
                />
              </Field>
              <Field label="Вес до беременности, кг" hint="Заполняется только врачом.">
                <input
                  inputMode="decimal"
                  value={settings.prePregnancyWeightKg}
                  onChange={(event) =>
                    setSettings({ ...settings, prePregnancyWeightKg: event.target.value })
                  }
                  placeholder="кг"
                />
              </Field>
              <Field label="Группа риска">
                <select
                  value={settings.riskGroup}
                  onChange={(event) => {
                    const riskGroup = event.target.value as RiskGroup;
                    setSettings({
                      ...settings,
                      riskGroup,
                      diaryRequired: riskGroup === "HIGH" ? true : settings.diaryRequired
                    });
                  }}
                >
                  <option value="LOW">Низкий</option>
                  <option value="HIGH">Высокий</option>
                </select>
              </Field>
              <Field label="Диабет">
                <select
                  value={settings.diabetesType}
                  onChange={(event) =>
                    setSettings({
                      ...settings,
                      diabetesType: event.target.value as DiabetesType
                    })
                  }
                >
                  <option value="NONE">Нет</option>
                  <option value="GESTATIONAL">Гестационный</option>
                  <option value="PREEXISTING">До беременности</option>
                </select>
              </Field>
              <Field label="Дневник обязателен">
                <select
                  value={settings.diaryRequired ? "yes" : "no"}
                  onChange={(event) =>
                    setSettings({ ...settings, diaryRequired: event.target.value === "yes" })
                  }
                >
                  <option value="no">Нет</option>
                  <option value="yes">Да</option>
                </select>
              </Field>
            </div>
            <Field label="Комментарий врача" hint="Виден только врачу.">
              <textarea
                value={settings.comment}
                onChange={(event) => setSettings({ ...settings, comment: event.target.value })}
                placeholder="Внутренняя заметка по пациентке"
                maxLength={4000}
              />
            </Field>
            <button className="btn btn-primary" disabled={savingSettings} onClick={() => void saveSettings()}>
              {savingSettings ? "Сохраняем…" : "Сохранить карточку"}
            </button>
          </div>

          <div className="card">
            <h2>Пороги алертов</h2>
            <p className="muted mt">
              Значения по умолчанию можно переопределить для этой пациентки.
            </p>
            <div className="grid two mt">
              {Object.keys(thresholds).map((code) => (
                <Field key={code} label={thresholdLabels[code] ?? code}>
                  <input
                    inputMode="decimal"
                    value={thresholds[code]}
                    onChange={(event) =>
                      setThresholds({ ...thresholds, [code]: event.target.value })
                    }
                  />
                </Field>
              ))}
            </div>
            <button className="btn btn-primary" disabled={savingThresholds} onClick={() => void saveThresholds()}>
              {savingThresholds ? "Сохраняем…" : "Сохранить пороги"}
            </button>
          </div>

          <div className="card">
            <h2>Удаление</h2>
            <p className="muted mt">
              Карточка, дневник, алерты, сообщения и привязка будут удалены без возможности
              восстановления.
            </p>
            <button className="btn btn-danger mt" onClick={() => void deleteCard()}>
              Удалить пациентку
            </button>
          </div>
        </>
      ) : null}

      {link && qr ? (
        <div className="modal-backdrop" onClick={() => setLink(null)}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <h2>QR для привязки</h2>
            <p className="muted mt">
              Покажите пациентке. Ссылка действует до {formatDateTime(link.expiresAt)} и один раз.
            </p>
            <div className="qr-box mt">
              <img src={qr} alt="QR для привязки" />
              <div style={{ flex: 1, minWidth: 220 }}>
                <p className="mono small" style={{ wordBreak: "break-all" }}>
                  {link.url}
                </p>
                <div className="row mt">
                  <button
                    className="btn btn-sm"
                    onClick={() => void navigator.clipboard?.writeText(link.url)}
                  >
                    Скопировать
                  </button>
                  <button className="btn btn-sm btn-ghost" onClick={() => setLink(null)}>
                    Закрыть
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
