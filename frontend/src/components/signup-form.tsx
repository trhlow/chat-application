import { useState } from "react";

import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";

import { FormField } from "@/components/form-field";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getErrorMessage } from "@/lib/errors";
import { signUpSchema } from "@/schemas/auth";
import { useAuthStore } from "@/store/auth-store";
import type { SignUpFormValues } from "@/types/auth";

export const SignupForm = () => {
  const navigate = useNavigate();
  const signup = useAuthStore((state) => state.signup);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const form = useForm<SignUpFormValues>({
    resolver: zodResolver(signUpSchema),
    defaultValues: { firstName: "", lastName: "", username: "", email: "", password: "" },
  });

  const onSubmit = async (values: SignUpFormValues) => {
    setSubmitError(null);
    try {
      await signup(values);
      navigate("/app", { replace: true });
    } catch (error) {
      setSubmitError(getErrorMessage(error, "Không thể tạo tài khoản lúc này."));
    }
  };

  return (
    <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
      <div className="grid grid-cols-2 gap-3">
        <FormField id="firstName" label="Họ" error={form.formState.errors.firstName?.message}>
          <Input id="firstName" placeholder="Nguyễn" autoComplete="given-name" {...form.register("firstName")} />
        </FormField>
        <FormField id="lastName" label="Tên" error={form.formState.errors.lastName?.message}>
          <Input id="lastName" placeholder="An" autoComplete="family-name" {...form.register("lastName")} />
        </FormField>
      </div>

      <FormField id="username" label="Tên đăng nhập" error={form.formState.errors.username?.message}>
        <Input id="username" placeholder="nguyenan" autoComplete="username" {...form.register("username")} />
      </FormField>

      <FormField id="email" label="Email" error={form.formState.errors.email?.message}>
        <Input id="email" type="email" placeholder="an@example.com" autoComplete="email" {...form.register("email")} />
      </FormField>

      <FormField id="password" label="Mật khẩu" error={form.formState.errors.password?.message}>
        <Input id="password" type="password" autoComplete="new-password" placeholder="Tạo mật khẩu an toàn" {...form.register("password")} />
      </FormField>

      {submitError ? (
        <div className="rounded-lg border border-red-500/25 bg-red-500/10 px-4 py-3 text-sm text-red-600 dark:text-red-300">
          {submitError}
        </div>
      ) : null}

      <Button type="submit" className="mt-2 h-11 w-full" disabled={form.formState.isSubmitting}>
        {form.formState.isSubmitting ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
        <ArrowRight className="h-4 w-4" />
      </Button>

      <p className="text-center text-sm text-muted-foreground">
        Đã có tài khoản?{" "}
        <Link className="font-semibold text-primary hover:underline" to="/signin">
          Đăng nhập
        </Link>
      </p>
    </form>
  );
};
