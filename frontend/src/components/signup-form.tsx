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
    defaultValues: {
      firstName: "",
      lastName: "",
      username: "",
      email: "",
      password: "",
    },
  });

  const onSubmit = async (values: SignUpFormValues) => {
    setSubmitError(null);

    try {
      await signup(values);
      navigate("/app", { replace: true });
    } catch (error) {
      setSubmitError(getErrorMessage(error, "Unable to create your account."));
    }
  };

  return (
    <form className="space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
      <div className="grid grid-cols-2 gap-3">
        <FormField
          id="firstName"
          label="Họ"
          error={form.formState.errors.firstName?.message}
        >
          <Input
            id="firstName"
            placeholder="Nguyễn"
            className="h-11 rounded-[14px] border-slate-200 bg-white px-4 text-[15px] shadow-none focus:border-[#7b2ff7] focus:ring-[#7b2ff7]/15"
            {...form.register("firstName")}
          />
        </FormField>

        <FormField
          id="lastName"
          label="Tên"
          error={form.formState.errors.lastName?.message}
        >
          <Input
            id="lastName"
            placeholder="An"
            className="h-11 rounded-[14px] border-slate-200 bg-white px-4 text-[15px] shadow-none focus:border-[#7b2ff7] focus:ring-[#7b2ff7]/15"
            {...form.register("lastName")}
          />
        </FormField>
      </div>

      <FormField
        id="username"
        label="Tên đăng nhập"
        error={form.formState.errors.username?.message}
      >
        <Input
          id="username"
          placeholder="moji"
          autoComplete="username"
          className="h-11 rounded-[14px] border-slate-200 bg-white px-4 text-[15px] shadow-none focus:border-[#7b2ff7] focus:ring-[#7b2ff7]/15"
          {...form.register("username")}
        />
      </FormField>

      <FormField
        id="email"
        label="Email"
        error={form.formState.errors.email?.message}
      >
        <Input
          id="email"
          type="email"
          placeholder="m@example.com"
          autoComplete="email"
          className="h-11 rounded-[14px] border-slate-200 bg-white px-4 text-[15px] shadow-none focus:border-[#7b2ff7] focus:ring-[#7b2ff7]/15"
          {...form.register("email")}
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
          autoComplete="new-password"
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
        {form.formState.isSubmitting ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
        <ArrowRight className="h-4 w-4" />
      </Button>

      <p className="text-center text-sm text-slate-500">
        Đã có tài khoản?{" "}
        <Link className="font-semibold text-slate-700 hover:text-[#7b2ff7]" to="/signin">
          Đăng nhập
        </Link>
      </p>
    </form>
  );
};
