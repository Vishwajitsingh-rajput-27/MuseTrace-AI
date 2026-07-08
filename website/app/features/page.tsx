import type { Metadata } from "next";
import { PageHero } from "@/components/page-hero";
import { MotionSection } from "@/components/motion-section";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { features } from "@/lib/site";
import { Sparkles } from "lucide-react";

export const metadata: Metadata = {
  title: "Features"
};

export default function FeaturesPage() {
  return (
    <>
      <PageHero
        eyebrow="Features"
        title="A complete Android tracing workflow"
        description="Generate, import, edit, quantize, preview, calibrate, and draw layer by layer with a floating overlay controller."
        icon={Sparkles}
      />
      <MotionSection className="container grid gap-4 pb-16 md:grid-cols-2 lg:grid-cols-3">
        {features.map((feature) => (
          <Card key={feature.title}>
            <CardHeader>
              <feature.icon className="h-5 w-5 text-primary" />
              <CardTitle>{feature.title}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm leading-7 text-muted-foreground">{feature.description}</p>
            </CardContent>
          </Card>
        ))}
      </MotionSection>
    </>
  );
}
