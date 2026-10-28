import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { ErrorBox } from "../components/ui";
import { PasswordInput } from "../components/PasswordInput";
import { request } from "../api";
import { useStaff } from "../session";

export default function LoginPage() {
  const { me, login } = useStaff();
  const navigate = useNavigate();
  const [email, setEmail] = useState(import.meta.env.DEV ? "doctor@example.com" : "");
  const [password, setPassword] = useState(import.meta.env.DEV ? "doctor123" : "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mustChange, setMustChange] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [tokenAfterLogin, setTokenAfterLogin] = useState<string | null>(null);

  if (me && !mustChange) {
    return <Navigate to="/cabinet" replace />;
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const response = await login(email.trim(), password);
      if (response.mustChangePassword) {
        setMustChange(true);
        setTokenAfterLogin(response.token);
      } else {
        navigate("/cabinet", { replace: true });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось войти");
    } finally {
      setBusy(false);
    }
  };

  const changePassword = async (event: FormEvent) => {
    event.preventDefault();
    if (!tokenAfterLogin) {
      return;
    }
    if (newPassword.length < 8) {
      setError("Пароль должен быть не короче 8 символов");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Пароли не совпадают");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await request<void>("/api/auth/change-password", {
        method: "POST",
        body: { currentPassword: password, newPassword },
        token: tokenAfterLogin
      });
      navigate("/cabinet", { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Не удалось изменить пароль");
    } finally {
      setBusy(false);
    }
  };

  const mismatch = confirmPassword.length > 0 && newPassword !== confirmPassword;
  const tooShort = newPassword.length > 0 && newPassword.length < 8;
  const cannotSave = busy || newPassword.length < 8 || confirmPassword.length < 8 || newPassword !== confirmPassword;

  return (
    <div className="shell">
      <main className="content narrow" style={{ paddingTop: 48 }}>
        <div className="card">
          <div className="brand" style={{ marginBottom: 6 }}>
            Антенатальный помощник
            <small>Кабинет врача</small>
          </div>

          {!mustChange ? (
            <form onSubmit={(event) => void submit(event)}>
              <h1 style={{ fontSize: 22, margin: "10px 0 16px" }}>Вход</h1>
              <label className="field">
                <span>Email</span>
                <input
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  autoComplete="username"
                  required
                />
              </label>
              <label className="field">
                <span>Пароль</span>
                <PasswordInput
                  name="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  autoComplete="current-password"
                  required
                />
              </label>
              {error ? <ErrorBox message={error} /> : null}
              <button className="btn btn-primary btn-block btn-lg" disabled={busy}>
                {busy ? "Входим…" : "Войти"}
              </button>
            </form>
          ) : (
            <form onSubmit={(event) => void changePassword(event)}>
              <h1 style={{ fontSize: 22, margin: "10px 0 10px" }}>Смените пароль</h1>
              <p className="muted" style={{ marginBottom: 14 }}>
                Это первый вход. Задайте новый пароль не короче 8 символов.
              </p>
              <label className="field">
                <span>Новый пароль</span>
                <PasswordInput
                  name="newPassword"
                  value={newPassword}
                  onChange={(event) => {
                    setNewPassword(event.target.value);
                    setError(null);
                  }}
                  minLength={8}
                  maxLength={72}
                  autoComplete="new-password"
                  required
                  aria-describedby={tooShort ? "password-too-short" : undefined}
                />
                {tooShort ? (
                  <p id="password-too-short" className="field-hint">
                    Не короче 8 символов
                  </p>
                ) : null}
              </label>
              <label className="field">
                <span>Повторите новый пароль</span>
                <PasswordInput
                  name="confirmPassword"
                  value={confirmPassword}
                  onChange={(event) => {
                    setConfirmPassword(event.target.value);
                    setError(null);
                  }}
                  minLength={8}
                  maxLength={72}
                  autoComplete="new-password"
                  required
                  aria-invalid={mismatch || undefined}
                  aria-describedby={mismatch ? "password-mismatch" : undefined}
                />
                {mismatch ? (
                  <p id="password-mismatch" className="field-error">
                    Пароли не совпадают
                  </p>
                ) : null}
              </label>
              {error ? <ErrorBox message={error} /> : null}
              <button className="btn btn-primary btn-block btn-lg" disabled={cannotSave}>
                {busy ? "Сохраняем…" : "Сохранить и продолжить"}
              </button>
            </form>
          )}
        </div>
        <p className="notice center">
          Для входа нужна учётная запись, созданная администратором клиники.
        </p>
      </main>
    </div>
  );
}
