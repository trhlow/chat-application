import type { Metadata } from "next";
import { Toaster } from "sonner";
import "@fontsource-variable/geist";

import "../index.css";

export const metadata: Metadata = {
  title: "InChat",
  description: "Realtime chat application",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi">
      <body>
        <Toaster richColors />
        {children}
      </body>
    </html>
  );
}
