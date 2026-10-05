import { describe, expect, it } from "vitest";

import { router } from "./router";

function findRouteByPath(
  routes: typeof router.routes,
  path: string,
): unknown {
  for (const route of routes) {
    if (route.path === path) {
      return route;
    }
    if (route.children) {
      const match = findRouteByPath(route.children, path);
      if (match) {
        return match;
      }
    }
  }
  return undefined;
}

describe("router", () => {
  it("exposes at least one route", () => {
    expect(router.routes.length).toBeGreaterThan(0);
  });

  it("has a catch-all route", () => {
    const catchAll = router.routes.find((r) => r.path === "*");
    expect(catchAll).toBeDefined();
  });

  it("does not expose runs outside the admin area", () => {
    expect(findRouteByPath(router.routes, "/runs")).toBeUndefined();
  });

  it("routes /admin/runs through the admin area", () => {
    expect(findRouteByPath(router.routes, "/admin/runs")).toBeDefined();
  });

  it("routes /admin/sources and /admin/locations through the admin area", () => {
    expect(findRouteByPath(router.routes, "/admin/sources")).toBeDefined();
    expect(findRouteByPath(router.routes, "/admin/locations")).toBeDefined();
  });

  it("routes /admin to the admin index", () => {
    expect(findRouteByPath(router.routes, "/admin")).toBeDefined();
  });

  it("routes /sources through the app shell", () => {
    expect(findRouteByPath(router.routes, "/sources")).toBeDefined();
  });

  it("routes /admin/login to the login page", () => {
    expect(findRouteByPath(router.routes, "/admin/login")).toBeDefined();
  });

  it("has no general login route", () => {
    expect(findRouteByPath(router.routes, "/accounts/login")).toBeUndefined();
  });

  it("routes / through the app shell", () => {
    expect(findRouteByPath(router.routes, "/")).toBeDefined();
  });
});
