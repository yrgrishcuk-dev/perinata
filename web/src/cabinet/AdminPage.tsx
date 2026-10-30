import { useCallback, useEffect, useState } from "react";
import { ErrorBox, Field, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDateTime } from "../format";
import { useStaff } from "../session";
import type { DoctorView, Role } from "../types";

export default function AdminPage() {
  const { me, token } = useStaff();
  const [doctors, setDoctors] = useState<DoctorView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("DOCTOR");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    setLoading(true);
    try {
      const data = await request<DoctorView[]>("/api/admin/doctors", { token });
      setDoctors(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить врачей");
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void load();
  }, [load]);

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
    return <ErrorBox message={error} onRetry={() => void load()} />;
  }

  const create = async () => {
    if (!token) {
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await request<DoctorView>("/api/admin/doctors", {
        method: "POST",
        body: { fullName: fullName.trim(), email: email.trim(), password, role },
        token
      });
      setFullName("");
      setEmail("");
      setPassword("");
      setRole("DOCTOR");
      setNotice("Врач создан. Пароль нужно сменить при первом входе.");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось создать врача");
    } finally {
      setBusy(false);
    }
  };

  const toggle = async (doctor: DoctorView) => {
    if (!token) {
      return;
    }
    const next = doctor.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    if (!window.confirm(`${next === "ACTIVE" ? "Включить" : "Отключить"} ${doctor.fullName}?`)) {
      return;
    }
    try {
      await request<DoctorView>(`/api/admin/doctors/${doctor.id}`, {
        method: "PATCH",
        body: { status: next },
        token
      });
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось изменить статус");
    }
  };

  const resetPassword = async (doctor: DoctorView) => {
    if (!token) {
      return;
    }
    const value = window.prompt(`Новый временный пароль для ${doctor.fullName} (не короче 8 символов)`);
    if (!value) {
      return;
    }
    try {
      await request<void>(`/api/admin/doctors/${doctor.id}/reset-password`, {
        method: "POST",
        body: { password: value },
        token
      });
      setNotice("Пароль сброшен. Передайте его врачу безопасным способом.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось сбросить пароль");
    }
  };

  return (
    <>
      <div className="page-head row between mb">
        <h1 style={{ fontSize: 24 }}>Врачи</h1>
        <button className="btn" onClick={() => void load()}>
          Обновить
        </button>
      </div>

      {notice ? <div className="alert ok">{notice}</div> : null}

      <div className="card">
        <h2>Добавить врача</h2>
        <p className="muted mt">
          Учётная запись создаётся вручную. Врач сменит временный пароль при первом входе.
        </p>
        <div className="grid two mt">
          <Field label="ФИО">
            <input value={fullName} onChange={(event) => setFullName(event.target.value)} />
          </Field>
          <Field label="Email">
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </Field>
          <Field label="Временный пароль">
            <input
              type="text"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="не короче 8 символов"
            />
          </Field>
          <Field label="Роль">
            <select value={role} onChange={(event) => setRole(event.target.value as Role)}>
              <option value="DOCTOR">Врач</option>
              <option value="ADMIN">Администратор</option>
            </select>
          </Field>
        </div>
        <button
          className="btn btn-primary"
          disabled={busy || !fullName.trim() || !email.trim() || password.length < 8}
          onClick={() => void create()}
        >
          {busy ? "Создаём…" : "Создать учётную запись"}
        </button>
      </div>

      <div className="card">
        <h2>Список</h2>
        <div className="table-wrap doctors-table mt">
          <table>
            <thead>
              <tr>
                <th>Врач</th>
                <th>Роль</th>
                <th>Статус</th>
                <th>Уведомления</th>
                <th>Последний вход</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {doctors.map((doctor) => (
                <tr key={doctor.id}>
                  <td>
                    <b>{doctor.fullName}</b>
                    <div className="muted small">{doctor.email}</div>
                  </td>
                  <td>{doctor.role === "ADMIN" ? "Админ" : "Врач"}</td>
                  <td>
                    {doctor.status === "ACTIVE" ? (
                      <span className="badge ok">Активен</span>
                    ) : (
                      <span className="badge neutral">Отключён</span>
                    )}
                  </td>
                  <td>{doctor.messengerLinked ? "Да" : "Нет"}</td>
                  <td>{formatDateTime(doctor.lastLoginAt)}</td>
                  <td>
                    <div className="row">
                      <button className="btn btn-sm" onClick={() => void resetPassword(doctor)}>
                        Сбросить пароль
                      </button>
                      <button className="btn btn-sm" onClick={() => void toggle(doctor)}>
                        {doctor.status === "ACTIVE" ? "Отключить" : "Включить"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="doctors-cards mt">
          {doctors.map((doctor) => (
            <div className="doctor-card" key={doctor.id}>
              <div className="row between">
                <b>{doctor.fullName}</b>
                {doctor.status === "ACTIVE" ? (
                  <span className="badge ok">Активен</span>
                ) : (
                  <span className="badge neutral">Отключён</span>
                )}
              </div>
              <div className="muted small" style={{ marginTop: 4, overflowWrap: "anywhere" }}>
                {doctor.email} · {doctor.role === "ADMIN" ? "Админ" : "Врач"}
              </div>
              <div className="muted small" style={{ marginTop: 6 }}>
                Уведомления: {doctor.messengerLinked ? "да" : "нет"} · Вход:{" "}
                {formatDateTime(doctor.lastLoginAt)}
              </div>
              <div className="row mt">
                <button className="btn btn-sm" onClick={() => void resetPassword(doctor)}>
                  Сбросить пароль
                </button>
                <button className="btn btn-sm" onClick={() => void toggle(doctor)}>
                  {doctor.status === "ACTIVE" ? "Отключить" : "Включить"}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}
