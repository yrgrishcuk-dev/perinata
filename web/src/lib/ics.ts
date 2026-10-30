import { appointmentTypeLabel } from "../format";
import type { AppointmentView } from "../types";

function escapeIcs(value: string): string {
  return value
    .replace(/\\/g, "\\\\")
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "")
    .replace(/;/g, "\\;")
    .replace(/,/g, "\\,");
}

function utc(value: Date): string {
  return value.toISOString().replace(/[-:]/g, "").replace(/\.\d{3}Z$/, "Z");
}

export function downloadAppointmentIcs(appointment: AppointmentView): void {
  const start = new Date(appointment.startAt);
  const end = new Date(start.getTime() + appointment.durationMinutes * 60_000);
  const summary = `Приём: ${appointmentTypeLabel(appointment.type)}`;
  const description = [
    appointment.comment,
    appointment.doctorName ? `Врач: ${appointment.doctorName}` : null,
    "Антенатальный помощник"
  ]
    .filter(Boolean)
    .join("\n");
  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//Antenatal//RU",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
    "BEGIN:VEVENT",
    `UID:appointment-${appointment.id}@perinata.ru`,
    `DTSTAMP:${utc(new Date())}`,
    `DTSTART:${utc(start)}`,
    `DTEND:${utc(end)}`,
    `SEQUENCE:${appointment.sequence}`,
    `SUMMARY:${escapeIcs(summary)}`,
    appointment.place ? `LOCATION:${escapeIcs(appointment.place)}` : null,
    description ? `DESCRIPTION:${escapeIcs(description)}` : null,
    "STATUS:CONFIRMED",
    "END:VEVENT",
    "END:VCALENDAR",
    ""
  ].filter((line): line is string => line !== null);

  const blob = new Blob([lines.join("\r\n")], {
    type: "text/calendar;charset=utf-8"
  });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `appointment-${appointment.id}.ics`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
