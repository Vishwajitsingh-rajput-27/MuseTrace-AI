"use client";

import Link from "next/link";
import { useState } from "react";
import { Github, Menu, Sparkles, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { downloadUrl, githubUrl, navItems } from "@/lib/site";

export function SiteHeader() {
  const [open, setOpen] = useState(false);

  return (
    <header className="sticky top-0 z-50 border-b border-border bg-background/88 backdrop-blur-xl">
      <div className="container flex h-16 items-center justify-between gap-4">
        <Link href="/" className="flex items-center gap-2 font-semibold tracking-normal">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl border border-border bg-card">
            <Sparkles className="h-4 w-4 text-primary" />
          </span>
          <span>MuseTrace AI</span>
        </Link>
        <nav className="hidden items-center gap-1 lg:flex">
          {navItems.slice(0, 7).map((item) => (
            <Button key={item.href} asChild variant="ghost" size="sm">
              <Link href={item.href}>{item.label}</Link>
            </Button>
          ))}
        </nav>
        <div className="flex items-center gap-2">
          <Button asChild variant="ghost" size="icon" className="hidden sm:inline-flex" aria-label="GitHub repository">
            <Link href={githubUrl}>
              <Github className="h-4 w-4" />
            </Link>
          </Button>
          <Button asChild size="sm">
            <Link href={downloadUrl}>Download APK</Link>
          </Button>
          <Button variant="outline" size="icon" className="lg:hidden" aria-label="Menu" onClick={() => setOpen((value) => !value)}>
            {open ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
          </Button>
        </div>
      </div>
      {open ? (
        <div className="container border-t border-border py-3 lg:hidden">
          <nav className="grid gap-1">
            {navItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setOpen(false)}
                className="rounded-xl px-3 py-2 text-sm text-muted-foreground hover:bg-secondary hover:text-foreground"
              >
                {item.label}
              </Link>
            ))}
            <Link
              href={githubUrl}
              onClick={() => setOpen(false)}
              className="rounded-xl px-3 py-2 text-sm text-muted-foreground hover:bg-secondary hover:text-foreground"
            >
              GitHub repository
            </Link>
          </nav>
        </div>
      ) : null}
    </header>
  );
}
