export type ConnectorState =
  | "unavailable" | "permission_required" | "connecting" | "connected"
  | "degraded" | "stopped" | "error";

export type RealtimeConnector = {
  provider: string;
  state: ConnectorState;
  consent: boolean;
  capabilities: string[];
  detail?: string;
};

export const connectorLabels = {
  google_meet_media_api: "Google Meet Media",
  user_consented_capture: "WhatsApp / Desktop Capture",
} as const;
