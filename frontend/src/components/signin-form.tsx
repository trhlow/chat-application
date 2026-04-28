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
    defaultValues: {
      emailOrUsername: "",
      password: "",
    },
  });

  const onSubmit = async (values: SignInFormValues) => {
    setSubmitError(null);

    try {
      await signin(values);
      navigate("/app", { replace: true });
    } catch (error) {
      setSubmitError(getErrorMessage(error, "Unable to sign in right now."));
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
          placeholder="moji"
          autoComplete="username"
          className="h-11 rounded-[14px] border-slate-200 bg-white px-4 text-[15px] shadow-none focus:border-[#7b2ff7] focus:ring-[#7b2ff7]/15"
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
          className="h-11 rounded-[14px] border-slate-200 bg-white px-4 text-[15px] shadow-none focus:border-[#7b2ff7] focus:ring-[#7b2ff7]/15"
          {...form.register("password")}
        />
      </FormField>

      {submitError ? (
        <div className="rounded-[16px] border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
          {submitError}
        </div>
      ) : null}

      <Button
        type="submit"
        className="mt-2 h-11 w-full rounded-full bg-[linear-gradient(90deg,#7b2ff7_0%,#8d32ff_100%)] text-sm font-semibold text-white shadow-[0_16px_26px_rgba(123,47,247,0.32)] hover:translate-y-0 hover:brightness-105"
        disabled={form.formState.isSubmitting}
      >
        {form.formState.isSubmitting ? "Đang đăng nhập..." : "Đăng nhập"}
        <ArrowRight className="h-4 w-4" />
      </Button>

      <p className="text-center text-sm text-slate-500">
        Chưa có tài khoản?{" "}
        <Link className="font-semibold text-slate-700 hover:text-[#7b2ff7]" to="/signup">
          Đăng ký
        </Link>
      </p>
    </form>
  );
};
