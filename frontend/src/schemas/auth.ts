import { z } from "zod";

export const signUpSchema = z.object({
  firstName: z.string().trim().min(1, "Vui lòng nhập họ."),
  lastName: z.string().trim().min(1, "Vui lòng nhập tên."),
  username: z
    .string()
    .trim()
    .min(3, "Tên đăng nhập cần ít nhất 3 ký tự.")
    .max(20, "Tên đăng nhập tối đa 20 ký tự.")
    .regex(
      /^[a-zA-Z0-9_]+$/,
      "Tên đăng nhập chỉ gồm chữ, số hoặc dấu gạch dưới.",
    ),
  email: z.string().trim().email("Vui lòng nhập email hợp lệ."),
  password: z.string().min(8, "Mật khẩu cần ít nhất 8 ký tự."),
});

export const signInSchema = z.object({
  emailOrUsername: z
    .string()
    .trim()
    .min(3, "Vui lòng nhập email hoặc tên đăng nhập."),
  password: z.string().min(8, "Mật khẩu cần ít nhất 8 ký tự."),
});
