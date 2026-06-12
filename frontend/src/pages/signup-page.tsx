import { AuthShell } from "@/components/auth-shell";
import { SignupForm } from "@/components/auth/signup-form";

export const SignUpPage = () => (
  <AuthShell
    mode="signup"
    title="Đăng ký tài khoản"
    description="Tạo tài khoản InChat để bắt đầu trò chuyện"
    footer={
      <>
        Tiếp tục đồng nghĩa với việc bạn chấp nhận điều khoản sử dụng InChat.
      </>
    }
  >
    <SignupForm />
  </AuthShell>
);
