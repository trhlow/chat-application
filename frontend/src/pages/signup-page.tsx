import { AuthShell } from "@/components/auth-shell";
import { SignupForm } from "@/components/signup-form";

export const SignUpPage = () => (
  <AuthShell
    mode="signup"
    title="Tạo tài khoản InChat"
    description="Chào mừng bạn! Hãy đăng ký để bắt đầu"
    footer={
      <>
        Bằng cách tiếp tục, bạn đồng ý với{" "}
        <span className="font-medium text-slate-600">Điều khoản dịch vụ</span> và{" "}
        <span className="font-medium text-slate-600">Chính sách bảo mật</span> của
        chúng tôi.
      </>
    }
  >
    <SignupForm />
  </AuthShell>
);
