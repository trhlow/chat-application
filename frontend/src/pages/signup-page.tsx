import { AuthShell } from "@/components/auth-shell";
import { SignupForm } from "@/components/signup-form";

export const SignUpPage = () => (
  <AuthShell
    mode="signup"
    title="Tạo tài khoản"
    description="Tham gia InChat và bắt đầu trò chuyện với bạn bè."
    footer={
      <>
        Khi tiếp tục, bạn đồng ý với <span className="font-medium text-foreground">Điều khoản dịch vụ</span> và{" "}
        <span className="font-medium text-foreground">Chính sách bảo mật</span>.
      </>
    }
  >
    <SignupForm />
  </AuthShell>
);
