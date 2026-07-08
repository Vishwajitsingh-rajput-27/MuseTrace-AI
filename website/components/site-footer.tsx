import Link from "next/link";
import { Github } from "lucide-react";
import { downloadUrl, githubUrl, navItems } from "@/lib/site";

export function SiteFooter() {
  return (
    <footer className="border-t border-border bg-card/30">
      <div className="container grid gap-8 py-10 md:grid-cols-[1.2fr_1fr]">
        <div>
          <p className="text-base font-semibold">MuseTrace AI</p>
          <p className="mt-2 max-w-xl text-sm leading-6 text-muted-foreground">
            Prompt. Layer. Trace. An Android-only drawing-assistance workflow for creating layered sketches and recreating them manually inside Instagram Draw.
          </p>
          <div className="mt-4 flex flex-wrap gap-3 text-sm">
            <Link className="text-primary hover:underline" href={downloadUrl}>
              Latest APK release
            </Link>
            <Link className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground" href={githubUrl}>
              <Github className="h-4 w-4" />
              GitHub
            </Link>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-2 text-sm">
          {navItems.map((item) => (
            <Link key={item.href} href={item.href} className="text-muted-foreground hover:text-foreground">
              {item.label}
            </Link>
          ))}
        </div>
      </div>
    </footer>
  );
}
