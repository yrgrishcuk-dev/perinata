import { bmiCategoryLabel, gainStatusLabel } from "../format";
import type { BmiCategory, GainView } from "../types";

interface Props {
  bmiCategory: BmiCategory | null;
  bmi: number | null;
  gain: GainView;
}

const TOTAL_GAIN: Record<BmiCategory, string> = {
  UNDERWEIGHT: "12,5–18 кг",
  NORMAL: "11,5–16 кг",
  OVERWEIGHT: "7–11,5 кг",
  OBESE: "5–9 кг"
};

export function GainAdvice({ bmiCategory, bmi, gain }: Props) {
  if (!bmiCategory) {
    return (
      <p className="notice mt">
        Рост и вес до беременности внесёт врач — после этого появится расчёт ИМТ и
        рекомендации по прибавке.
      </p>
    );
  }

  return (
    <div className="gain-advice mt">
      <p className="muted small">
        ИМТ до беременности: {bmi?.toFixed(1) ?? "—"} · {bmiCategoryLabel(bmiCategory)}
      </p>
      <p className="muted small">
        Рекомендуемая прибавка за всю беременность: {TOTAL_GAIN[bmiCategory]}. В первом
        триместре прибавка обычно небольшая, во втором и третьем — по коридору.
      </p>
      <p className="small">
        Сейчас: {gain.gainKg != null ? `${gain.gainKg.toFixed(1)} кг` : "нет данных"} ·{" "}
        <b className={gainStatusLabel(gain.status) === "В коридоре" ? "ok-text" : ""}>
          {gainStatusLabel(gain.status)}
        </b>
      </p>
    </div>
  );
}
