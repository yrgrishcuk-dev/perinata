/// <reference types="vite/client" />

interface TelegramWebApp {
  initData: string;
  initDataUnsafe?: {
    start_param?: string;
    user?: { id: number; first_name?: string; username?: string };
  };
  colorScheme?: string;
  ready(): void;
  expand(): void;
  setHeaderColor?(color: string): void;
  HapticFeedback?: {
    impactOccurred(style: "light" | "medium" | "heavy" | "rigid" | "soft"): void;
  };
}

interface MaxWebApp {
  initData: string;
  initDataUnsafe?: {
    query_id?: string;
    auth_date?: number;
    start_param?: string;
    user?: {
      id: number;
      first_name?: string;
      last_name?: string;
      username?: string;
      language_code?: string;
    };
    chat?: {
      id: number;
      type?: "DIALOG" | "CHAT" | "CHANNEL";
    };
  };
  platform?: string;
  version?: string;
  deviceName?: string;
  BackButton?: {
    show(): void;
    hide(): void;
  };
}

interface Window {
  Telegram?: {
    WebApp?: TelegramWebApp;
  };
  WebApp?: MaxWebApp;
}
