import { AlertOctagon, AlertTriangle, CheckCircle2, Eye } from "lucide-react";
import type { Urgency } from "../lib/kb";

const ICON = { critical: AlertOctagon, high: AlertTriangle, medium: Eye, none: CheckCircle2 };

/** Urgency is told three ways: colour, icon and words. Colour alone fails in sunlight and for colour-blind users. */
export function UrgencyBand({ urgency, label }: { urgency: Urgency; label: string }) {
  const Icon = ICON[urgency];
  return (
    <p className={`urgency urgency-${urgency}`}>
      <Icon aria-hidden />
      <span>{label}</span>
    </p>
  );
}
