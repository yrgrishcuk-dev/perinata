export type BmiCategory = "UNDERWEIGHT" | "NORMAL" | "OVERWEIGHT" | "OBESE";
export type RiskGroup = "LOW" | "HIGH";
export type DiabetesType = "NONE" | "GESTATIONAL" | "PREEXISTING";
export type PatientStatus = "ACTIVE" | "CLOSED";
export type Role = "ADMIN" | "DOCTOR";
export type Severity = "INFO" | "WARNING" | "CRITICAL";
export type AlarmStatus = "NEW" | "ACKED" | "CLOSED";
export type AlarmType =
  | "SYMPTOM"
  | "BP"
  | "GLUCOSE"
  | "CONTRACTION"
  | "WEIGHT"
  | "MISSED_DIARY"
  | "WATERS";
export type GlucoseContext = "FASTING" | "POSTPRANDIAL";
export type MessageDirection = "TO_PATIENT" | "TO_DOCTOR";
export type AuthorType = "DOCTOR" | "PATIENT" | "SYSTEM";

export interface PatientSummary {
  id: number;
  publicCode: string;
  fullName: string;
  ageYears: number;
  parity: number | null;
  edd: string;
  gestationalAge: string;
  gestationalWeek: number;
  bmi: number | null;
  bmiCategory: BmiCategory | null;
  bmiCategoryLabel: string | null;
  riskGroup: RiskGroup;
  diabetesType: DiabetesType;
  diaryRequired: boolean;
  status: PatientStatus;
  bound: boolean;
  lastDiaryDate: string | null;
  openAlerts: number;
  createdAt: string;
}

export interface CorridorPoint {
  week: number;
  minGainKg: number;
  maxGainKg: number;
}

export interface GainPoint {
  date: string;
  weightKg: number;
  week: number;
  gainKg: number;
}

export interface GainView {
  week: number;
  day: number;
  ageLabel: string;
  minGainKg: number | null;
  maxGainKg: number | null;
  gainKg: number | null;
  status: "BELOW" | "WITHIN" | "ABOVE" | "UNKNOWN";
  points: GainPoint[];
  corridor: CorridorPoint[];
}

export interface WeekContent {
  id: number;
  weekFrom: number;
  weekTo: number;
  version: string;
  title: string;
  fetalText: string;
  nutritionText: string;
  warningText: string;
}

export interface Meal {
  meal: string;
  items: string;
}

export interface RationView {
  id: number;
  category: BmiCategory;
  categoryLabel: string;
  version: string;
  title: string;
  kcal: number;
  proteinG: number;
  fatG: number;
  carbsG: number;
  meals: Meal[];
}

export interface DiaryView {
  id: number;
  date: string;
  sbp: number | null;
  dbp: number | null;
  pulse: number | null;
  weightKg: number | null;
  glucoseMmol: number | null;
  glucoseContext: GlucoseContext | null;
  note: string | null;
  updatedAt: string;
}

export interface AlertView {
  id: number;
  patientId: number | null;
  patientName: string | null;
  publicCode: string | null;
  type: AlarmType;
  severity: Severity;
  payload: string | null;
  status: AlarmStatus;
  comment: string | null;
  createdAt: string;
  ackedAt: string | null;
}

export interface DiarySaveResponse {
  entry: DiaryView;
  alert: AlertView | null;
  message: string;
}

export interface MessageView {
  id: number;
  direction: MessageDirection;
  authorType: AuthorType;
  body: string;
  createdAt: string;
  readAt: string | null;
}

export interface MessagesResponse {
  messages: MessageView[];
  unread: number;
}

export interface ContractionPoint {
  id: number;
  startAt: string;
  endAt: string;
  durationSeconds: number;
}

export interface ContractionState {
  sessionId: number | null;
  startedAt: string | null;
  endedAt: string | null;
  countLastHour: number;
  averageIntervalSeconds: number | null;
  averageDurationSeconds: number | null;
  severity: Severity;
  message: string;
  contractions: ContractionPoint[];
}

export interface SymptomResponse {
  critical: boolean;
  message: string;
  symptoms: string[];
  reportedAt: string;
}

export interface ContentBundle {
  week: number;
  content: WeekContent | null;
  ration: RationView | null;
}

export interface PatientHome {
  summary: PatientSummary;
  gain: GainView;
  week: WeekContent | null;
  ration: RationView | null;
  todayDiary: DiaryView | null;
  unreadMessages: number;
  doctorName: string | null;
}

export interface PatientMe {
  bound: boolean;
  home: PatientHome | null;
}

export interface ProfileResponse {
  summary: PatientSummary;
  doctorName: string | null;
  reminderTime: string;
  heightCm: number | null;
  prePregnancyWeightKg: number | null;
  consentVersion: string;
  consentAt: string;
}

export interface InvitePreview {
  fullName: string;
  ageYears: number;
  edd: string;
  doctorName: string | null;
  alreadyBound: boolean;
  alreadyBoundToThisCard: boolean;
}

export interface InviteConfirmResponse {
  bound: boolean;
  patient: PatientSummary;
}

export interface PatientDetails {
  summary: PatientSummary;
  thresholds: Record<string, number>;
  gain: GainView;
  doctorName: string | null;
  boundPlatform: string | null;
  consentVersion: string | null;
  consentAt: string | null;
  hasMessengerNotifications: boolean;
  heightCm: number | null;
  prePregnancyWeightKg: number | null;
  reminderTime: string;
  comment: string | null;
}

export interface DashboardView {
  patients: number;
  openAlerts: number;
  diaryToday: number;
  highRisk: number;
}

export interface DoctorView {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  status: string;
  messengerLinked: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface LoginResponse {
  token: string;
  role: Role;
  fullName: string;
  doctorId: number;
  mustChangePassword: boolean;
}

export interface StaffMe {
  id: number;
  fullName: string;
  email: string;
  role: Role;
  clinicId: number;
}

export interface LinkResponse {
  url: string;
  expiresAt: string;
}

export interface MessengerLinkResponse {
  url: string;
  linked: boolean;
}

export interface AuditLog {
  id: number;
  actorType: string;
  actorId: string | null;
  action: string;
  entityType: string | null;
  entityId: string | null;
  metadata: string | null;
  ip: string | null;
  createdAt: string;
}

export interface PatientCreateInput {
  fullName: string;
  ageYears: number;
  parity?: number | null;
  edd: string;
  heightCm?: number | null;
  prePregnancyWeightKg?: number | null;
  riskGroup?: RiskGroup;
  diabetesType?: DiabetesType;
  diaryRequired?: boolean;
  reminderTime?: string;
  comment?: string | null;
}

export type PatientUpdateInput = Partial<PatientCreateInput>;

export interface CalendarEventView {
  id: number;
  date: string;
  time: string | null;
  title: string;
  note: string | null;
  patientId: number | null;
  patientName: string | null;
  patientPublicCode: string | null;
}

export interface CalendarEventInput {
  date: string;
  title: string;
  time?: string | null;
  patientId?: number | null;
  note?: string | null;
}
