import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  ONBOARDING_STEPS,
  completedSteps,
  loadOnboarding,
  saveOnboarding,
  setOnboardingDismissed,
  type OnboardingState,
  type OnboardingStep
} from "../lib/onboarding";

interface Props {
  doctorId: number;
  patientsCount: number;
}

export function GettingStartedCard({ doctorId, patientsCount }: Props) {
  const [state, setState] = useState<OnboardingState>(() => loadOnboarding(doctorId));

  useEffect(() => {
    setState(loadOnboarding(doctorId));
  }, [doctorId]);

  useEffect(() => {
    if (patientsCount <= 0) {
      return;
    }
    const current = loadOnboarding(doctorId);
    if (current.steps.patient) {
      return;
    }
    const next: OnboardingState = {
      ...current,
      steps: { ...current.steps, patient: true }
    };
    saveOnboarding(doctorId, next);
    setState(next);
  }, [doctorId, patientsCount]);

  if (!doctorId || state.dismissed) {
    return null;
  }

  const done = completedSteps(state);
  const total = ONBOARDING_STEPS.length;
  const percent = Math.round((done / total) * 100);

  const toggle = (step: OnboardingStep) => {
    const current = loadOnboarding(doctorId);
    const next: OnboardingState = {
      ...current,
      steps: { ...current.steps, [step]: !current.steps[step] }
    };
    saveOnboarding(doctorId, next);
    setState(next);
  };

  const hide = () => {
    setState(setOnboardingDismissed(doctorId, true));
  };

  return (
    <div className="card getting-started">
      <div className="row between">
        <div>
          <h2>С чего начать</h2>
          <p className="muted" style={{ marginTop: 4 }}>
            Пять шагов, чтобы запустить кабинет и провести первую пациентку.
          </p>
        </div>
        <span className={`badge ${done === total ? "ok" : "neutral"}`}>
          {done} из {total}
        </span>
      </div>

      <div className="progress-track" aria-hidden="true">
        <span style={{ width: `${percent}%` }} />
      </div>

      <div className="onboarding-steps">
        {ONBOARDING_STEPS.map((step) => {
          const checked = state.steps[step.key];
          return (
            <div className={`onboarding-step ${checked ? "done" : ""}`} key={step.key}>
              <input
                type="checkbox"
                checked={checked}
                onChange={() => toggle(step.key)}
                aria-label={step.title}
              />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div className="onboarding-step-title">{step.title}</div>
                <div className="muted small">{step.description}</div>
              </div>
              <Link className="btn btn-ghost btn-sm" to={step.to}>
                Открыть
              </Link>
            </div>
          );
        })}
      </div>

      <button className="btn btn-ghost btn-sm mt" type="button" onClick={hide}>
        Скрыть чек-лист
      </button>
    </div>
  );
}
