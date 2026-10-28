import { Link, Navigate, Route, Routes } from "react-router-dom";
import { PatientProvider, StaffProvider } from "./session";
import MiniLayout from "./mini/MiniLayout";
import ConnectPage from "./mini/ConnectPage";
import InvitePage from "./mini/InvitePage";
import OnboardingPage from "./mini/OnboardingPage";
import HomePage from "./mini/HomePage";
import DiaryPage from "./mini/DiaryPage";
import SymptomsPage from "./mini/SymptomsPage";
import ContractionsPage from "./mini/ContractionsPage";
import WeekPage from "./mini/WeekPage";
import MessagesPage from "./mini/MessagesPage";
import ProfilePage from "./mini/ProfilePage";
import LoginPage from "./cabinet/LoginPage";
import CabinetLayout from "./cabinet/CabinetLayout";
import DashboardPage from "./cabinet/DashboardPage";
import PatientsPage from "./cabinet/PatientsPage";
import PatientPage from "./cabinet/PatientPage";
import AlertsPage from "./cabinet/AlertsPage";
import AdminPage from "./cabinet/AdminPage";
import AuditPage from "./cabinet/AuditPage";

function NotFound() {
  return (
    <div className="shell">
      <main className="content narrow">
        <div className="card">
          <h1 style={{ fontSize: 22, marginBottom: 8 }}>Страница не найдена</h1>
          <Link className="btn btn-primary mt" to="/app">
            На главную
          </Link>
        </div>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/app" replace />} />

      <Route
        path="/connect"
        element={
          <PatientProvider>
            <ConnectPage />
          </PatientProvider>
        }
      />
      <Route
        path="/invite"
        element={
          <PatientProvider>
            <InvitePage />
          </PatientProvider>
        }
      />
      <Route
        path="/onboarding"
        element={
          <PatientProvider>
            <OnboardingPage />
          </PatientProvider>
        }
      />

      <Route
        path="/app"
        element={
          <PatientProvider>
            <MiniLayout />
          </PatientProvider>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="diary" element={<DiaryPage />} />
        <Route path="symptoms" element={<SymptomsPage />} />
        <Route path="contractions" element={<ContractionsPage />} />
        <Route path="week" element={<WeekPage />} />
        <Route path="messages" element={<MessagesPage />} />
        <Route path="profile" element={<ProfilePage />} />
      </Route>

      <Route
        path="/cabinet/login"
        element={
          <StaffProvider>
            <LoginPage />
          </StaffProvider>
        }
      />
      <Route
        path="/cabinet"
        element={
          <StaffProvider>
            <CabinetLayout />
          </StaffProvider>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="patients" element={<PatientsPage />} />
        <Route path="patients/:id" element={<PatientPage />} />
        <Route path="alerts" element={<AlertsPage />} />
        <Route path="admin" element={<AdminPage />} />
        <Route path="audit" element={<AuditPage />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
