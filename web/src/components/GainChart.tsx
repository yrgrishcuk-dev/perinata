import type { GainView } from "../types";

interface Props {
  gain: GainView;
  height?: number;
}

export function GainChart({ gain, height = 240 }: Props) {
  const corridor = gain.corridor ?? [];
  const points = gain.points ?? [];
  if (corridor.length === 0 && points.length === 0) {
    return <div className="muted">Недостаточно данных для графика.</div>;
  }

  const width = 640;
  const padding = { left: 38, right: 14, top: 14, bottom: 26 };
  const innerW = width - padding.left - padding.right;
  const innerH = height - padding.top - padding.bottom;

  const values: number[] = [0];
  corridor.forEach((c) => {
    values.push(c.minGainKg, c.maxGainKg);
  });
  points.forEach((p) => values.push(p.gainKg));
  const rawMin = Math.min(...values);
  const rawMax = Math.max(...values);
  const yMin = Math.min(0, Math.floor(rawMin - 1));
  const yMax = Math.max(4, Math.ceil(rawMax + 1));
  const xMax = 42;

  const x = (week: number) => padding.left + (Math.max(0, Math.min(xMax, week)) / xMax) * innerW;
  const y = (value: number) =>
    padding.top + innerH - ((value - yMin) / (yMax - yMin || 1)) * innerH;

  const minPath = corridor
    .map((c) => `${x(c.week).toFixed(1)},${y(c.minGainKg).toFixed(1)}`)
    .join(" ");
  const maxPath = [...corridor]
    .reverse()
    .map((c) => `${x(c.week).toFixed(1)},${y(c.maxGainKg).toFixed(1)}`)
    .join(" ");
  const area = corridor.length
    ? `M ${minPath.replace(/ /g, " L ")} L ${maxPath.replace(/ /g, " L ")} Z`
    : "";

  const line = points
    .filter((p) => p.gainKg != null)
    .map((p, index) => `${index === 0 ? "M" : "L"} ${x(p.week).toFixed(1)} ${y(p.gainKg).toFixed(1)}`)
    .join(" ");

  const weekTicks = [0, 10, 20, 30, 40];
  const gainTicks = [yMin, 0, yMax].filter((v, i, arr) => arr.indexOf(v) === i && v >= yMin && v <= yMax);

  return (
    <svg
      className="chart"
      viewBox={`0 0 ${width} ${height}`}
      style={{ height }}
      role="img"
      aria-label="График прибавки веса"
    >
      {gainTicks.map((tick) => (
        <g key={`y-${tick}`}>
          <line
            className="grid-line"
            x1={padding.left}
            x2={padding.left + innerW}
            y1={y(tick)}
            y2={y(tick)}
          />
          <text x={padding.left - 6} y={y(tick) + 3} textAnchor="end">
            {tick}
          </text>
        </g>
      ))}
      {weekTicks.map((tick) => (
        <g key={`x-${tick}`}>
          <line
            className="grid-line"
            x1={x(tick)}
            x2={x(tick)}
            y1={padding.top}
            y2={padding.top + innerH}
          />
          <text x={x(tick)} y={height - 8} textAnchor="middle">
            {tick}
          </text>
        </g>
      ))}
      {area ? <path className="corridor" d={area} /> : null}
      {line ? <path className="line" d={line} /> : null}
      {points.map((p) => (
        <circle key={`${p.date}-${p.week}`} className="dot" cx={x(p.week)} cy={y(p.gainKg)} r={4} />
      ))}
      <line
        className="current"
        x1={x(gain.week)}
        x2={x(gain.week)}
        y1={padding.top}
        y2={padding.top + innerH}
      />
      <text x={x(gain.week) + 4} y={padding.top + 10} textAnchor="start">
        {gain.week} нед
      </text>
      <text x={padding.left} y={height - 8} textAnchor="start">
        недели
      </text>
    </svg>
  );
}
