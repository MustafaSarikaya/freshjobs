import { useState, type FormEvent } from "react";

import {
  useCreateSource,
  useDeleteSource,
  useUpdateSource,
  type SourceInput,
} from "@/api/admin";
import { useSources } from "@/api/sources";
import { PLATFORM_LABELS, SourceStatusBadge } from "@/components/SourceTable";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { ApiError } from "@/lib/fetcher";
import type { Source, SourcePlatform } from "@/types/api";

const EMPTY_INPUT: SourceInput = {
  name: "",
  platform: "GREENHOUSE",
  board_id: "",
  is_active: true,
};

type Editing = { id: number | null; input: SourceInput };

export function AdminSourcesPage() {
  const { data: sources = [], isLoading, isError } = useSources();
  const [editing, setEditing] = useState<Editing | null>(null);
  const [deleting, setDeleting] = useState<Source | null>(null);

  return (
    <section className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-normal">Sources</h1>
          <p className="text-sm text-muted-foreground">
            Job boards the scraper ingests. Inactive sources are skipped.
          </p>
        </div>
        <Button type="button" onClick={() => setEditing({ id: null, input: EMPTY_INPUT })}>
          Add source
        </Button>
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading sources…</p>
      ) : null}

      {isError ? (
        <p role="alert" className="text-sm text-destructive">
          Could not load sources.
        </p>
      ) : null}

      {!isLoading && !isError && sources.length === 0 ? (
        <p className="rounded-md border border-dashed border-border p-6 text-sm text-muted-foreground">
          No sources yet.
        </p>
      ) : null}

      {!isLoading && !isError && sources.length > 0 ? (
        <div className="responsive-data-table rounded-md lg:border lg:border-border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Platform</TableHead>
                <TableHead>Board ID</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="lg:text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {sources.map((source) => (
                <TableRow key={source.id}>
                  <TableCell data-label="Name" className="font-medium">
                    {source.name}
                  </TableCell>
                  <TableCell data-label="Platform">
                    {PLATFORM_LABELS[source.platform]}
                  </TableCell>
                  <TableCell data-label="Board ID" className="break-all font-mono text-xs">
                    {source.board_id}
                  </TableCell>
                  <TableCell data-label="Status">
                    <SourceStatusBadge active={source.is_active} />
                  </TableCell>
                  <TableCell data-label="Actions" className="space-x-2 lg:text-right">
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      aria-label={`Edit ${source.name}`}
                      onClick={() =>
                        setEditing({
                          id: source.id,
                          input: {
                            name: source.name,
                            platform: source.platform,
                            board_id: source.board_id,
                            is_active: source.is_active,
                          },
                        })
                      }
                    >
                      Edit
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      aria-label={`Delete ${source.name}`}
                      onClick={() => setDeleting(source)}
                    >
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      ) : null}

      {editing ? (
        <SourceFormDialog editing={editing} onClose={() => setEditing(null)} />
      ) : null}
      {deleting ? (
        <DeleteSourceDialog source={deleting} onClose={() => setDeleting(null)} />
      ) : null}
    </section>
  );
}

function SourceFormDialog({ editing, onClose }: { editing: Editing; onClose: () => void }) {
  const createSource = useCreateSource();
  const updateSource = useUpdateSource();
  const [input, setInput] = useState(editing.input);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const cleaned = { ...input, name: input.name.trim(), board_id: input.board_id.trim() };
    if (!cleaned.name || !cleaned.board_id) {
      setError("Name and board ID are required.");
      return;
    }
    setError(null);
    try {
      if (editing.id === null) {
        await createSource.mutateAsync(cleaned);
      } else {
        await updateSource.mutateAsync({ id: editing.id, input: cleaned });
      }
      onClose();
    } catch (err) {
      setError(
        err instanceof ApiError && err.status === 409
          ? "Another source already uses this board."
          : "Could not save the source. Try again.",
      );
    }
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <form className="space-y-4" onSubmit={handleSubmit} noValidate>
          <DialogHeader>
            <DialogTitle>{editing.id === null ? "Add source" : "Edit source"}</DialogTitle>
            <DialogDescription>
              The board ID is the company slug on the job board platform.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="source-name">Name</Label>
            <Input
              id="source-name"
              value={input.name}
              onChange={(e) => setInput({ ...input, name: e.target.value })}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="source-platform">Platform</Label>
            <select
              id="source-platform"
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 text-sm"
              value={input.platform}
              onChange={(e) =>
                setInput({ ...input, platform: e.target.value as SourcePlatform })
              }
            >
              {Object.entries(PLATFORM_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="source-board">Board ID</Label>
            <Input
              id="source-board"
              value={input.board_id}
              onChange={(e) => setInput({ ...input, board_id: e.target.value })}
            />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={input.is_active}
              onChange={(e) => setInput({ ...input, is_active: e.target.checked })}
            />
            Active
          </label>
          {error ? (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          ) : null}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit">Save</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function DeleteSourceDialog({ source, onClose }: { source: Source; onClose: () => void }) {
  const deleteSource = useDeleteSource();
  const [error, setError] = useState<string | null>(null);

  async function handleDelete() {
    setError(null);
    try {
      await deleteSource.mutateAsync(source.id);
      onClose();
    } catch {
      setError("Could not delete the source. Try again.");
    }
  }

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Delete source</DialogTitle>
          <DialogDescription className="break-words">
            Delete “{source.name}” and all of its job listings? This cannot be undone.
            To stop ingesting it but keep its listings, mark it inactive instead.
          </DialogDescription>
        </DialogHeader>
        {error ? (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        ) : null}
        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button type="button" variant="destructive" onClick={handleDelete}>
            Delete
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
