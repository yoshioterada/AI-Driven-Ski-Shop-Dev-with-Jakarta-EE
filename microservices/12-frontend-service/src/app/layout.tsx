import type { Metadata } from "next";
// import { Inter } from "next/font/google";
import "./globals.css";
import { AuthProvider } from "@/hooks/useAuth";
import IntegratedServices from "@/components/layout/IntegratedServices";
import DummyUserControlPanel from "@/components/dev/DummyUserControlPanel";

// const inter = Inter({
//   subsets: ["latin"],
//   variable: "--font-inter",
// });

export const metadata: Metadata = {
  title: "スキーリゾート管理システム",
  description: "スキーリゾートECサイト - AIサポート＆ポイントシステム搭載",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ja">
      <body
        className="antialiased"
      >
        <AuthProvider>
          <IntegratedServices>
            {children}
          </IntegratedServices>
          <DummyUserControlPanel />
        </AuthProvider>
      </body>
    </html>
  );
}
