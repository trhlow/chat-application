import { AuthShell } from "@/components/auth-shell";
import { SigninForm } from "@/components/signin-form";

export const SignInPage = () => (
  <AuthShell
    mode="signin"
    title="Chào mừng quay lại"
    description="Đăng nhập để tiếp tục các cuộc trò chuyện của bạn."
    footer={
      <>
        Khi tiếp tục, bạn đồng ý với <span className="font-medium text-foreground">Điều khoản dịch vụ</span> và{" "}
        <span className="font-medium text-foreground">Chính sách bảo mật</span>.
      </>
    }
  >
    <SigninForm />
  </AuthShell>
);
