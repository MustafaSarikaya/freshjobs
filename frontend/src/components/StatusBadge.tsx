import { cn } from "@/lib/utils";
import type { RunStatus } from "@/types/api";

const TONES: Record<RunStatus, string> = {
  completed: "bg-green-100 text-green-900 dark:bg-green-900/40 dark:text-green-200",
  failed: "bg-red-100 text-red-900 dark:bg-red-900/40 dark:text-red-200",
  running: "bg-blue-100 text-blue-900 dark:bg-blue-900/40 dark:text-blue-200",
  pending: "bg-muted text-muted-foreground",
};

export function StatusBadge({ status }: { status: RunStatus | string }) {
  const tone = TONES[status as RunStatus] ?? "bg-muted text-muted-foreground";
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium",
        tone,
      )}
    >
      {status}
    </span>
  );
}
