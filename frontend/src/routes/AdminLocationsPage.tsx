import { useState } from "react";

import {
  useAdminLocations,
  useUpdateLocationTag,
  type AdminLocationTag,
} from "@/api/admin";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export function AdminLocationsPage() {
  const { data: tags = [], isLoading, isError } = useAdminLocations();
  const [query, setQuery] = useState("");
  const [missingOnly, setMissingOnly] = useState(false);

  const needle = query.trim().toLowerCase();
  const visible = tags.filter(
    (tag) =>
      tag.name.toLowerCase().includes(needle) && (!missingOnly || !tag.countryCode),
  );

  return (
    <section className="space-y-6">
      <div className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-normal">Location tags</h1>
        <p className="text-sm text-muted-foreground">
          Correct the geography the geocoder assigned. Edited tags are never re-geocoded.
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-4">
        <Input
          aria-label="Search location tags"
          placeholder="Search…"
          className="max-w-xs"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <label className="flex items-center gap-2 text-sm">
          <input
            type="checkbox"
            checked={missingOnly}
            onChange={(e) => setMissingOnly(e.target.checked)}
          />
          Only tags without a country
        </label>
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading location tags…</p>
      ) : null}

      {isError ? (
        <p role="alert" className="text-sm text-destructive">
          Could not load location tags.
        </p>
      ) : null}

      {!isLoading && !isError && tags.length === 0 ? (
        <p className="rounded-md border border-dashed border-border p-6 text-sm text-muted-foreground">
          No location tags yet.
        </p>
      ) : null}

      {!isLoading && !isError && tags.length > 0 ? (
        <div className="responsive-data-table rounded-md lg:border lg:border-border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Country</TableHead>
                <TableHead>Region</TableHead>
                <TableHead>City</TableHead>
                <TableHead className="lg:text-right">Save</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {visible.map((tag) => (
                <LocationRow key={tag.id} tag={tag} />
              ))}
            </TableBody>
          </Table>
        </div>
      ) : null}
    </section>
  );
}

type RowStatus = "idle" | "saved" | "failed";

function LocationRow({ tag }: { tag: AdminLocationTag }) {
  const updateTag = useUpdateLocationTag();
  const [country, setCountry] = useState(tag.countryCode ?? "");
  const [region, setRegion] = useState(tag.regionCode ?? "");
  const [city, setCity] = useState(tag.city ?? "");
  const [status, setStatus] = useState<RowStatus>("idle");

  async function save() {
    try {
      await updateTag.mutateAsync({
        id: tag.id,
        update: {
          countryCode: country.trim() || null,
          regionCode: region.trim() || null,
          city: city.trim() || null,
        },
      });
      setStatus("saved");
    } catch {
      setStatus("failed");
    }
  }

  return (
    <TableRow>
      <TableCell data-label="Name" className="font-medium">
        {tag.name}
      </TableCell>
      <TableCell data-label="Country">
        <Input
          aria-label={`Country for ${tag.name}`}
          className="w-20"
          maxLength={2}
          value={country}
          onChange={(e) => setCountry(e.target.value)}
        />
      </TableCell>
      <TableCell data-label="Region">
        <Input
          aria-label={`Region for ${tag.name}`}
          className="w-28"
          maxLength={10}
          value={region}
          onChange={(e) => setRegion(e.target.value)}
        />
      </TableCell>
      <TableCell data-label="City">
        <Input
          aria-label={`City for ${tag.name}`}
          value={city}
          onChange={(e) => setCity(e.target.value)}
        />
      </TableCell>
      <TableCell data-label="Save" className="space-x-2 whitespace-nowrap lg:text-right">
        {status === "saved" ? (
          <span className="text-xs text-muted-foreground">Saved</span>
        ) : null}
        {status === "failed" ? (
          <span className="text-xs text-destructive">Failed</span>
        ) : null}
        <Button type="button" size="sm" variant="outline" onClick={save}>
          Save
        </Button>
      </TableCell>
    </TableRow>
  );
}
