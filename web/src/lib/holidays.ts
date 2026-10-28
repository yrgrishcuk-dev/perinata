import { toIsoDate } from "./calendar";

const NEW_YEAR = "Новогодние каникулы";
const CHRISTMAS = "Рождество";
const DEFENDER = "День защитника Отечества";
const WOMEN = "Международный женский день";
const SPRING = "Праздник Весны и Труда";
const VICTORY = "День Победы";
const RUSSIA = "День России";
const UNITY = "День народного единства";

function yearHolidays(year: number): Record<string, string> {
  const dates: Record<string, string> = {};
  for (let day = 1; day <= 8; day += 1) {
    dates[`${year}-01-0${day}`] = day === 7 ? CHRISTMAS : NEW_YEAR;
  }
  dates[`${year}-02-23`] = DEFENDER;
  dates[`${year}-03-08`] = WOMEN;
  dates[`${year}-05-01`] = SPRING;
  dates[`${year}-05-09`] = VICTORY;
  dates[`${year}-06-12`] = RUSSIA;
  dates[`${year}-11-04`] = UNITY;
  return dates;
}

const HOLIDAYS: Record<string, string> = {
  ...yearHolidays(2026),
  ...yearHolidays(2027)
};

export function holidayLabel(date: Date): string | null {
  return HOLIDAYS[toIsoDate(date)] ?? null;
}

export function isHoliday(date: Date): boolean {
  return holidayLabel(date) !== null;
}
