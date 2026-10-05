import { readFileSync, statSync } from "node:fs";
import path from "node:path";

const SITE_URL = "https://freshjobs.up.railway.app/";
const frontendDir = path.resolve(__dirname, "..");
const html = readFileSync(path.join(frontendDir, "index.html"), "utf8");
const doc = new DOMParser().parseFromString(html, "text/html");

function meta(attr: "property" | "name", key: string): string {
  const content = doc
    .querySelector(`meta[${attr}="${key}"]`)
    ?.getAttribute("content");
  expect(content, `<meta ${attr}="${key}">`).toBeTruthy();
  return content as string;
}

function readPngHeader(file: string) {
  const bytes = readFileSync(file);
  expect(bytes.subarray(1, 4).toString("ascii")).toBe("PNG");
  return {
    width: bytes.readUInt32BE(16),
    height: bytes.readUInt32BE(20),
    colorType: bytes[25],
  };
}

describe("social preview meta tags", () => {
  it("describes the page for Open Graph crawlers", () => {
    expect(meta("property", "og:type")).toBe("website");
    expect(meta("property", "og:url")).toBe(SITE_URL);
    expect(meta("property", "og:title")).toContain("Fresh Jobs");
    expect(meta("property", "og:description")).not.toHaveLength(0);
    expect(meta("name", "description")).toBe(
      meta("property", "og:description"),
    );
  });

  it("points og:image at an absolute HTTPS URL on the live site", () => {
    const image = new URL(meta("property", "og:image"));
    expect(image.protocol).toBe("https:");
    expect(image.origin).toBe(new URL(SITE_URL).origin);
    expect(meta("property", "og:image:secure_url")).toBe(image.href);
  });

  it("uses the large Twitter card with the same image", () => {
    expect(meta("name", "twitter:card")).toBe("summary_large_image");
    expect(meta("name", "twitter:title")).toBe(meta("property", "og:title"));
    expect(meta("name", "twitter:description")).toBe(
      meta("property", "og:description"),
    );
    expect(meta("name", "twitter:image")).toBe(meta("property", "og:image"));
    expect(meta("name", "twitter:image:alt")).toBe(
      meta("property", "og:image:alt"),
    );
  });

  it("ships an image that matches its declared type and size", () => {
    const { pathname } = new URL(meta("property", "og:image"));
    const file = path.join(frontendDir, "public", pathname);
    const png = readPngHeader(file);

    expect(meta("property", "og:image:type")).toBe("image/png");
    expect(Number(meta("property", "og:image:width"))).toBe(png.width);
    expect(Number(meta("property", "og:image:height"))).toBe(png.height);
    expect([png.width, png.height]).toEqual([1200, 630]);
    expect(png.colorType).toBe(2);
    expect(statSync(file).size).toBeLessThan(300 * 1024);
  });
});
