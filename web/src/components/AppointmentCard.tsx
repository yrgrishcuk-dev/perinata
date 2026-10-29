import {
  appointmentTypeLabel,
  appointmentWhen,
  patientResponseClass,
  patientResponseLabel
} from "../format";
import { downloadAppointmentIcs } from "../lib/ics";
import type { AppointmentView, PatientResponse } from "../types";

interface AppointmentCardProps {
  appointment: AppointmentView;
  onRespond?: (response: Extract<PatientResponse, "CONFIRMED" | "DECLINED">) => void;
  busy?: boolean;
  compact?: boolean;
}

export function AppointmentCard({
  appointment,
  onRespond,
  busy = false,
  compact = false
}: AppointmentCardProps) {
  const future = new Date(appointment.startAt).getTime() > Date.now();
  const active = appointment.status === "SCHEDULED" && future;
  const cancelled = appointment.status === "CANCELLED";

  return (
    <div className={`appointment-card${compact ? " compact" : ""}`}>
      <div className="row between">
        <div>
          <span className="badge neutral">{appointmentTypeLabel(appointment.type)}</span>
          <b className="appointment-when">
            {appointmentWhen(appointment.date, appointment.time)}
          </b>
        </div>
        <span
          className={`badge ${
            cancelled ? "danger" : patientResponseClass(appointment.patientResponse)
          }`}
        >
          {cancelled ? "Отменён" : patientResponseLabel(appointment.patientResponse)}
        </span>
      </div>

      {appointment.place ? <p className="muted small mt">Место: {appointment.place}</p> : null}
      {appointment.comment ? <p className="mt">{appointment.comment}</p> : null}
      {appointment.doctorName ? (
        <p className="muted small">Врач: {appointment.doctorName}</p>
      ) : null}

      {active || (!cancelled && future) ? (
        <div className="row appointment-actions">
          {onRespond ? (
            <>
              <button
                className="btn btn-primary btn-sm"
                type="button"
                disabled={busy || appointment.patientResponse === "CONFIRMED"}
                onClick={() => onRespond("CONFIRMED")}
              >
                Подтвердить
              </button>
              <button
                className="btn btn-sm"
                type="button"
                disabled={busy || appointment.patientResponse === "DECLINED"}
                onClick={() => onRespond("DECLINED")}
              >
                Не смогу прийти
              </button>
            </>
          ) : null}
          {!cancelled ? (
            <button
              className="btn btn-ghost btn-sm"
              type="button"
              onClick={() => downloadAppointmentIcs(appointment)}
            >
              В календарь
            </button>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
