import { AuthShell } from "@/components/auth-shell";
import { SigninForm } from "@/components/auth/signin-form";

export const SignInPage = () => (
  <AuthShell
    mode="signin"
    title="Đăng nhập với mật khẩu"
    description="Đăng nhập InChat để kết nối với bạn bè"
    footer={
      <>
        Tiếp tục đồng nghĩa với việc bạn chấp nhận điều khoản sử dụng InChat.
      </>
    }
  >
    <SigninForm />
  </AuthShell>
);
