import { useState } from "react";

import { useTriggerRun } from "@/api/admin";
import { useRuns } from "@/api/runs";
import { StatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

const MONTHS = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];

function twoDigits(value: number) {
  return value.toString().padStart(2, "0");
}

function formatTimestamp(value: string | null) {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  return `${MONTHS[date.getUTCMonth()]} ${twoDigits(date.getUTCDate())}, ${date.getUTCFullYear()} ${twoDigits(date.getUTCHours())}:${twoDigits(date.getUTCMinutes())}`;
}

export function RunsPage() {
  const { data: runs = [], isLoading, isError } = useRuns();
  const triggerRun = useTriggerRun();
  const [triggerMessage, setTriggerMessage] = useState<string | null>(null);

  async function handleTrigger() {
    setTriggerMessage(null);
    try {
      await triggerRun.mutateAsync();
      setTriggerMessage("Ingestion started. It will appear below shortly.");
    } catch {
      setTriggerMessage("Could not start ingestion.");
    }
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-normal">Ingestion Runs</h1>
          <p className="text-sm text-muted-foreground">
            Recent scraping runs and their persisted result counts.
          </p>
        </div>
        <Button type="button" onClick={handleTrigger} disabled={triggerRun.isPending}>
          {triggerRun.isPending ? "Starting…" : "Run ingestion now"}
        </Button>
      </div>

      {triggerMessage ? (
        <p className="text-sm text-muted-foreground">{triggerMessage}</p>
      ) : null}

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading runs…</p>
      ) : null}

      {isError ? (
        <p role="alert" className="text-sm text-destructive">
          Could not load ingestion runs.
        </p>
      ) : null}

      {!isLoading && !isError && runs.length === 0 ? (
        <p className="rounded-md border border-dashed border-border p-6 text-sm text-muted-foreground">
          No ingestion runs yet.
        </p>
      ) : null}

      {!isLoading && !isError && runs.length > 0 ? (
        <div className="responsive-data-table rounded-md lg:border lg:border-border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Started</TableHead>
                <TableHead>Finished</TableHead>
                <TableHead className="lg:text-right">Sources</TableHead>
                <TableHead className="lg:text-right">Created</TableHead>
                <TableHead className="lg:text-right">Updated</TableHead>
                <TableHead className="lg:text-right">Expired</TableHead>
                <TableHead>Error</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {runs.map((run) => (
                <TableRow key={run.id}>
                  <TableCell data-label="ID" className="font-medium">
                    #{run.id}
                  </TableCell>
                  <TableCell data-label="Status">
                    <StatusBadge status={run.status} />
                  </TableCell>
                  <TableCell data-label="Started">
                    {formatTimestamp(run.started_at)}
                  </TableCell>
                  <TableCell data-label="Finished">
                    {formatTimestamp(run.finished_at)}
                  </TableCell>
                  <TableCell data-label="Sources" className="lg:text-right">
                    {run.sources_processed}
                  </TableCell>
                  <TableCell data-label="Created" className="lg:text-right">
                    {run.listings_created}
                  </TableCell>
                  <TableCell data-label="Updated" className="lg:text-right">
                    {run.listings_updated}
                  </TableCell>
                  <TableCell data-label="Expired" className="lg:text-right">
                    {run.listings_expired}
                  </TableCell>
                  <TableCell
                    data-label="Error"
                    className="break-words lg:max-w-sm lg:truncate"
                  >
                    {run.error_message ?? "—"}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      ) : null}
    </section>
  );
}
