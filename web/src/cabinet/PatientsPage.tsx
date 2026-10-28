import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ErrorBox, Field, RiskBadge, Spinner } from "../components/ui";
import { request } from "../api";
import { formatDate } from "../format";
import { useStaff } from "../session";
import type { DiabetesType, PatientCreateInput, PatientSummary, RiskGroup } from "../types";

function defaultEdd(): string {
  const date = new Date();
  date.setDate(date.getDate() + 210);
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

interface FormState {
  fullName: string;
  ageYears: string;
  parity: string;
  edd: string;
  heightCm: string;
  prePregnancyWeightKg: string;
  riskGroup: RiskGroup;
  diabetesType: DiabetesType;
  diaryRequired: boolean;
}

const emptyForm = (): FormState => ({
  fullName: "",
  ageYears: "28",
  parity: "",
  edd: defaultEdd(),
  heightCm: "",
  prePregnancyWeightKg: "",
  riskGroup: "LOW",
  diabetesType: "NONE",
  diaryRequired: false
});

export default function PatientsPage() {
  const { token } = useStaff();
  const navigate = useNavigate();
  const [patients, setPatients] = useState<PatientSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const load = async () => {
    if (!token) {
      return;
    }
    setLoading(true);
    try {
      const data = await request<PatientSummary[]>("/api/doctor/patients", { token });
      setPatients(data);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось загрузить пациенток");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  const filtered = useMemo(() => {
    const value = query.trim().toLowerCase();
    if (!value) {
      return patients;
    }
    return patients.filter(
      (patient) =>
        patient.fullName.toLowerCase().includes(value) ||
        patient.publicCode.toLowerCase().includes(value)
    );
  }, [patients, query]);

  if (loading) {
    return <Spinner />;
  }
  if (error) {
    return <ErrorBox message={error} onRetry={() => void load()} />;
  }

  const submit = async () => {
    if (!token) {
      return;
    }
    setSaving(true);
    setFormError(null);
    try {
      const body: PatientCreateInput = {
        fullName: form.fullName.trim(),
        ageYears: Number(form.ageYears),
        parity: form.parity ? Number(form.parity) : null,
        edd: form.edd,
        heightCm: form.heightCm ? Number(form.heightCm.replace(",", ".")) : null,
        prePregnancyWeightKg: form.prePregnancyWeightKg
          ? Number(form.prePregnancyWeightKg.replace(",", "."))
          : null,
        riskGroup: form.riskGroup,
        diabetesType: form.diabetesType,
        diaryRequired: form.diaryRequired
      };
      const created = await request<PatientSummary>("/api/doctor/patients", {
        method: "POST",
        body,
        token
      });
      setShowCreate(false);
      setForm(emptyForm());
      navigate(`/cabinet/patients/${created.id}`);
    } catch (e) {
      setFormError(e instanceof Error ? e.message : "Не удалось создать карточку");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="row between mb">
        <h1 style={{ fontSize: 24 }}>Пациентки</h1>
        <button className="btn btn-primary" onClick={() => setShowCreate(true)}>
          Новая пациентка
        </button>
      </div>

      <div className="card">
        <label className="field" style={{ marginBottom: 10 }}>
          <span>Поиск по ФИО или коду</span>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Иванова или A7K2"
          />
        </label>

        {filtered.length === 0 ? (
          <p className="muted">Пациенток пока нет. Создайте первую карточку.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Пациентка</th>
                  <th>Срок</th>
                  <th>ПДР</th>
                  <th>Риск</th>
                  <th>Дневник</th>
                  <th>События</th>
                  <th>Привязка</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {filtered.map((patient) => (
                  <tr key={patient.id}>
                    <td>
                      <b>{patient.fullName}</b>
                      <div className="muted small mono">{patient.publicCode}</div>
                    </td>
                    <td>{patient.gestationalAge}</td>
                    <td>{formatDate(patient.edd)}</td>
                    <td>
                      <RiskBadge risk={patient.riskGroup} />
                    </td>
                    <td>{patient.lastDiaryDate ? formatDate(patient.lastDiaryDate) : "—"}</td>
                    <td>
                      {patient.openAlerts > 0 ? (
                        <span className="badge danger">{patient.openAlerts}</span>
                      ) : (
                        <span className="badge neutral">0</span>
                      )}
                    </td>
                    <td>
                      {patient.bound ? (
                        <span className="badge ok">Да</span>
                      ) : (
                        <span className="badge neutral">Нет</span>
                      )}
                    </td>
                    <td>
                      <Link className="btn btn-sm" to={`/cabinet/patients/${patient.id}`}>
                        Открыть
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showCreate ? (
        <div className="modal-backdrop" onClick={() => setShowCreate(false)}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <h2 style={{ marginBottom: 14 }}>Новая карточка</h2>
            <Field label="ФИО">
              <input
                value={form.fullName}
                onChange={(event) => setForm({ ...form, fullName: event.target.value })}
                placeholder="Иванова Анна Сергеевна"
              />
            </Field>
            <div className="grid two">
              <Field label="Возраст">
                <input
                  inputMode="numeric"
                  value={form.ageYears}
                  onChange={(event) => setForm({ ...form, ageYears: event.target.value })}
                />
              </Field>
              <Field label="Какие по счёту роды">
                <input
                  inputMode="numeric"
                  value={form.parity}
                  onChange={(event) => setForm({ ...form, parity: event.target.value })}
                  placeholder="1"
                />
              </Field>
              <Field label="ПДР">
                <input
                  type="date"
                  value={form.edd}
                  onChange={(event) => setForm({ ...form, edd: event.target.value })}
                />
              </Field>
              <Field label="Рост, см (если известен)">
                <input
                  inputMode="decimal"
                  value={form.heightCm}
                  onChange={(event) => setForm({ ...form, heightCm: event.target.value })}
                  placeholder="165"
                />
              </Field>
              <Field label="Вес до беременности, кг">
                <input
                  inputMode="decimal"
                  value={form.prePregnancyWeightKg}
                  onChange={(event) =>
                    setForm({ ...form, prePregnancyWeightKg: event.target.value })
                  }
                  placeholder="62"
                />
              </Field>
              <Field label="Группа риска">
                <select
                  value={form.riskGroup}
                  onChange={(event) => {
                    const riskGroup = event.target.value as RiskGroup;
                    setForm({
                      ...form,
                      riskGroup,
                      diaryRequired: riskGroup === "HIGH" ? true : form.diaryRequired
                    });
                  }}
                >
                  <option value="LOW">Низкий</option>
                  <option value="HIGH">Высокий</option>
                </select>
              </Field>
              <Field label="Диабет">
                <select
                  value={form.diabetesType}
                  onChange={(event) =>
                    setForm({ ...form, diabetesType: event.target.value as DiabetesType })
                  }
                >
                  <option value="NONE">Нет</option>
                  <option value="GESTATIONAL">Гестационный</option>
                  <option value="PREEXISTING">До беременности</option>
                </select>
              </Field>
              <Field label="Дневник обязателен">
                <select
                  value={form.diaryRequired ? "yes" : "no"}
                  onChange={(event) =>
                    setForm({ ...form, diaryRequired: event.target.value === "yes" })
                  }
                >
                  <option value="no">Нет</option>
                  <option value="yes">Да</option>
                </select>
              </Field>
            </div>
            <p className="notice" style={{ marginBottom: 12 }}>
              Время напоминания о дневнике и пороги настраиваются в карточке пациентки после
              создания.
            </p>
            {formError ? <ErrorBox message={formError} /> : null}
            <div className="row">
              <button className="btn btn-primary" disabled={saving} onClick={() => void submit()}>
                {saving ? "Создаём…" : "Создать карточку"}
              </button>
              <button className="btn btn-ghost" onClick={() => setShowCreate(false)}>
                Отмена
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
