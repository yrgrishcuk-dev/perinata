import type { ReactNode } from "react";

export function MiniShell({
  children,
  subtitle
}: {
  children: ReactNode;
  subtitle?: string;
}) {
  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          Антенатальный помощник
          {subtitle ? <small>{subtitle}</small> : null}
        </div>
      </header>
      <main className="content narrow">{children}</main>
    </div>
  );
}
