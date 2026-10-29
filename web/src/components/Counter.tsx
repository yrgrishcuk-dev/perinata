interface CounterProps {
  value: string;
  onChange: (value: string) => void;
  min?: number;
  max?: number;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

export function Counter({ value, onChange, min = 1, max = 20 }: CounterProps) {
  const parsed = Number(value);
  const current = Number.isFinite(parsed) && parsed > 0 ? parsed : min;

  return (
    <div className="counter">
      <button
        type="button"
        aria-label="Уменьшить"
        disabled={current <= min}
        onClick={() => onChange(String(clamp(current - 1, min, max)))}
      >
        −
      </button>
      <input
        inputMode="numeric"
        value={value}
        aria-label="Значение"
        onChange={(event) => {
          const digits = event.target.value.replace(/\D/g, "");
          onChange(digits === "" ? "" : String(clamp(Number(digits), min, max)));
        }}
        onBlur={() => {
          if (value === "") {
            onChange(String(min));
          }
        }}
      />
      <button
        type="button"
        aria-label="Увеличить"
        disabled={current >= max}
        onClick={() => onChange(String(clamp(current + 1, min, max)))}
      >
        +
      </button>
    </div>
  );
}
