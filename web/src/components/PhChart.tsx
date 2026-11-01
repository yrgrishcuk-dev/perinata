import type { DiaryView } from "../types";

interface Props {
  entries: DiaryView[];
  height?: number;
}

export function PhChart({ entries, height = 200 }: Props) {
  const points = entries
    .filter((entry) => entry.vaginalPh != null)
    .slice()
    .sort((a, b) => a.date.localeCompare(b.date));

  if (points.length === 0) {
    return <div className="muted mt">Недостаточно данных для графика pH.</div>;
  }

  const width = 640;
  const padding = { left: 34, right: 14, top: 14, bottom: 24 };
  const innerW = width - padding.left - padding.right;
  const innerH = height - padding.top - padding.bottom;
  const yMin = 3;
  const yMax = 7;
  const x = (index: number) =>
    padding.left + (points.length === 1 ? innerW / 2 : (index / (points.length - 1)) * innerW);
  const y = (value: number) =>
    padding.top + innerH - ((value - yMin) / (yMax - yMin)) * innerH;
  const line = points
    .map((entry, index) => `${index === 0 ? "M" : "L"} ${x(index).toFixed(1)} ${y(entry.vaginalPh ?? yMin).toFixed(1)}`)
    .join(" ");

  return (
    <svg
      className="chart"
      viewBox={`0 0 ${width} ${height}`}
      style={{ height }}
      role="img"
      aria-label="График pH влагалища"
    >
      {[3, 4, 5, 6, 7].map((tick) => (
        <g key={tick}>
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
      <line
        className="current"
        x1={padding.left}
        x2={padding.left + innerW}
        y1={y(5)}
        y2={y(5)}
      />
      <path className="line" d={line} />
      {points.map((entry, index) => (
        <circle
          key={entry.id}
          className="dot"
          cx={x(index)}
          cy={y(entry.vaginalPh ?? yMin)}
          r={4}
        />
      ))}
      <text x={padding.left} y={height - 7} textAnchor="start">
        {points[0].date}
      </text>
      <text x={padding.left + innerW} y={height - 7} textAnchor="end">
        {points[points.length - 1].date}
      </text>
      <text x={padding.left + innerW} y={y(5) - 4} textAnchor="end">
        порог 5.0
      </text>
    </svg>
  );
}
