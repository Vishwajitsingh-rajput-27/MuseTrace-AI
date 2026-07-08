import type { Metadata } from "next";
import "./globals.css";
import { SiteFooter } from "@/components/site-footer";
import { SiteHeader } from "@/components/site-header";
import { appName, tagline } from "@/lib/site";

export const metadata: Metadata = {
  title: {
    default: `${appName} - ${tagline}`,
    template: `%s - ${appName}`
  },
  description: "MuseTrace AI converts images into layered sketch workflows for user-confirmed Android drawing assistance inside Instagram Draw.",
  metadataBase: new URL("https://vishwajitsingh-rajput-27.github.io")
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className="dark">
      <body>
        <SiteHeader />
        <main>{children}</main>
        <SiteFooter />
      </body>
    </html>
  );
}
