import { useState } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";

import { FormField } from "@/components/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getErrorMessage } from "@/lib/errors";
import { signInSchema } from "@/schemas/auth";
import { useAuthStore } from "@/store/auth-store";
import type { SignInFormValues } from "@/types/auth";

export const SigninForm = () => {
  const navigate = useNavigate();
  const signin = useAuthStore((state) => state.signin);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const form = useForm<SignInFormValues>({
    resolver: zodResolver(signInSchema),
    defaultValues: { emailOrUsername: "", password: "" },
  });

  const onSubmit = async (values: SignInFormValues) => {
    setSubmitError(null);
    try {
      await signin(values);
      navigate("/app", { replace: true });
    } catch (error) {
      setSubmitError(getErrorMessage(error, "Không thể đăng nhập lúc này."));
    }
  };

  return (
    <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
      <FormField
        id="emailOrUsername"
        label="Tên đăng nhập"
        error={form.formState.errors.emailOrUsername?.message}
      >
        <Input
          id="emailOrUsername"
          placeholder="Tên đăng nhập hoặc email"
          autoComplete="username"
          {...form.register("emailOrUsername")}
        />
      </FormField>

      <FormField
        id="password"
        label="Mật khẩu"
        error={form.formState.errors.password?.message}
      >
        <Input
          id="password"
          type="password"
          autoComplete="current-password"
          placeholder="Nhập mật khẩu của bạn"
          {...form.register("password")}
        />
      </FormField>

      {submitError ? (
        <div className="rounded-lg border border-red-500/25 bg-red-500/10 px-4 py-3 text-sm text-red-600 dark:text-red-300">
          {submitError}
        </div>
      ) : null}

      <Button type="submit" className="mt-2 h-11 w-full" disabled={form.formState.isSubmitting}>
        {form.formState.isSubmitting ? "Đang đăng nhập..." : "Đăng nhập"}
        <ArrowRight className="h-4 w-4" />
      </Button>

      <p className="text-center text-sm text-muted-foreground">
        Chưa có tài khoản?{" "}
        <Link className="font-semibold text-primary hover:underline" to="/signup">
          Đăng ký
        </Link>
      </p>
    </form>
  );
};
