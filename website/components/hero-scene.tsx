import { Layers3, Palette, Play, ShieldCheck } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function HeroScene() {
  const palette = ["#2f80ed", "#9b8cff", "#f2c94c", "#27ae60", "#eb5757", "#f2994a"];
  return (
    <div className="relative min-h-[560px] overflow-hidden border-y border-border bg-card/30">
      <div className="surface-grid absolute inset-0 opacity-60" />
      <div className="container relative grid min-h-[560px] items-center gap-6 py-10 lg:grid-cols-[1fr_420px]">
        <div className="max-w-2xl">
          <Badge variant="outline" className="gap-2">
            <SparkDot />
            Android-only drawing assistance
          </Badge>
          <h1 className="mt-6 text-5xl font-semibold tracking-normal md:text-7xl">MuseTrace AI</h1>
          <p className="mt-4 text-xl text-muted-foreground">Prompt. Layer. Trace.</p>
          <p className="mt-6 max-w-xl text-base leading-8 text-muted-foreground">
            Convert gallery, camera, and Gemini-generated images into semi-realistic layered sketches, then recreate them inside Instagram Draw with a floating overlay and user-confirmed Accessibility gestures.
          </p>
        </div>
        <div className="relative">
          <Card className="shadow-soft">
            <CardHeader className="border-b border-border">
              <div className="flex items-center justify-between gap-3">
                <CardTitle className="text-base">Overlay session</CardTitle>
                <Badge variant="secondary">Color 4 of 16</Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-5 p-5">
              <div className="rounded-xl border border-border bg-background p-4">
                <div className="flex items-center gap-4">
                  <div className="h-20 w-20 rounded-2xl border border-border" style={{ backgroundColor: "#9b8cff" }} />
                  <div>
                    <p className="font-medium">Soft violet layer</p>
                    <p className="mt-1 text-sm text-muted-foreground">HEX #9B8CFF</p>
                    <p className="text-sm text-muted-foreground">RGB 155, 140, 255</p>
                  </div>
                </div>
                <p className="mt-4 text-sm leading-6 text-muted-foreground">
                  Select this color manually in Instagram Draw, then tap Continue.
                </p>
              </div>
              <div className="grid grid-cols-3 gap-3 text-sm">
                <Metric label="Strokes" value="218" />
                <Metric label="Time" value="3m 40s" />
                <Metric label="Progress" value="25%" />
              </div>
              <div className="flex flex-wrap gap-2">
                {palette.map((color) => (
                  <span key={color} className="h-9 w-9 rounded-full border border-border" style={{ backgroundColor: color }} />
                ))}
              </div>
              <div className="grid gap-2 sm:grid-cols-2">
                <div className="flex items-center gap-2 rounded-xl border border-border bg-secondary p-3 text-sm">
                  <Play className="h-4 w-4 text-primary" />
                  Continue after color selection
                </div>
                <div className="flex items-center gap-2 rounded-xl border border-border bg-secondary p-3 text-sm">
                  <ShieldCheck className="h-4 w-4 text-primary" />
                  No Instagram login
                </div>
              </div>
            </CardContent>
          </Card>
          <div className="mt-4 grid grid-cols-2 gap-3">
            <MiniPanel icon={Layers3} label="16/24/32 color layers" />
            <MiniPanel icon={Palette} label="Manual color workflow" />
          </div>
        </div>
      </div>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-secondary/70 p-3">
      <p className="text-muted-foreground">{label}</p>
      <p className="mt-1 font-semibold">{value}</p>
    </div>
  );
}

function MiniPanel({ icon: Icon, label }: { icon: LucideIcon; label: string }) {
  return (
    <div className="flex items-center gap-2 rounded-2xl border border-border bg-card/80 p-3 text-sm text-muted-foreground">
      <Icon className="h-4 w-4 text-primary" />
      {label}
    </div>
  );
}

function SparkDot() {
  return <span className="h-2 w-2 rounded-full bg-primary" />;
}
