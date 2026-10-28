import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { ErrorBox } from "../components/ui";
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
                <input
                  type="password"
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
                <input
                  type="password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  minLength={8}
                  autoComplete="new-password"
                  required
                />
              </label>
              {error ? <ErrorBox message={error} /> : null}
              <button className="btn btn-primary btn-block btn-lg" disabled={busy}>
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
