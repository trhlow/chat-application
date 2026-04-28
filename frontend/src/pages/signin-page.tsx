import { AuthShell } from "@/components/auth-shell";
import { SigninForm } from "@/components/signin-form";

export const SignInPage = () => (
  <AuthShell
    mode="signin"
    title="Chào mừng quay lại"
    description="Đăng nhập vào tài khoản InChat của bạn"
    footer={
      <>
        Bằng cách tiếp tục, bạn đồng ý với{" "}
        <span className="font-medium text-slate-600">Điều khoản dịch vụ</span> và{" "}
        <span className="font-medium text-slate-600">Chính sách bảo mật</span> của
        chúng tôi.
      </>
    }
  >
    <SigninForm />
  </AuthShell>
);
