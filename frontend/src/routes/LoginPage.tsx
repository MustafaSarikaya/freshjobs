import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useSearchParams } from "react-router-dom";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { LoginError, login } from "@/lib/auth";

const loginSchema = z.object({
  username: z.string().min(1, "Username is required"),
  password: z.string().min(1, "Password is required"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

type LoginPageProps = {
  onRedirect?: (url: string) => void;
};

/* v8 ignore start -- unit tests inject onRedirect; Playwright covers browser navigation. */
function browserRedirect(url: string) {
  window.location.assign(url);
}
/* v8 ignore stop */

export function LoginPage({
  onRedirect = browserRedirect,
}: LoginPageProps) {
  const [searchParams] = useSearchParams();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const next = searchParams.get("next") ?? "/admin/";
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  async function onSubmit(values: LoginFormValues) {
    setSubmitError(null);
    try {
      const redirectTo = await login({ ...values, next });
      onRedirect(redirectTo);
    } catch (error) {
      setSubmitError(
        error instanceof LoginError ? error.message : "Sign in failed.",
      );
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-6 text-foreground">
      <section className="w-full max-w-sm space-y-6 rounded-md border border-border p-4 shadow-sm sm:p-6">
        <div className="space-y-1">
          <p className="text-sm font-medium text-muted-foreground">Fresh Jobs</p>
          <h1 className="text-2xl font-semibold tracking-normal">Sign in</h1>
        </div>

        {submitError ? (
          <p
            role="alert"
            className="rounded-md bg-destructive/10 p-3 text-sm text-destructive"
          >
            {submitError}
          </p>
        ) : null}

        <Form {...form}>
          <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
            <FormField
              control={form.control}
              name="username"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Username</FormLabel>
                  <FormControl>
                    <Input
                      autoComplete="username"
                      autoFocus
                      type="text"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="password"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Password</FormLabel>
                  <FormControl>
                    <Input
                      autoComplete="current-password"
                      type="password"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <Button
              className="w-full"
              disabled={form.formState.isSubmitting}
              type="submit"
            >
              Sign in
            </Button>
          </form>
        </Form>
      </section>
    </main>
  );
}
