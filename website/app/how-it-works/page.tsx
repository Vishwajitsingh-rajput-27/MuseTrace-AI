import type { Metadata } from "next";
import { Route } from "lucide-react";
import { MotionSection } from "@/components/motion-section";
import { PageHero } from "@/components/page-hero";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { workflowSteps } from "@/lib/site";

export const metadata: Metadata = {
  title: "How It Works"
};

export default function HowItWorksPage() {
  return (
    <>
      <PageHero
        eyebrow="Workflow timeline"
        title="How MuseTrace AI turns art into guided layers"
        description="The Android app handles image preparation and stroke planning. You stay in control of Instagram and confirm every drawing step."
        icon={Route}
      />
      <MotionSection className="container pb-16">
        <div className="grid gap-4">
          {workflowSteps.map((step, index) => (
            <Card key={step.title} className="overflow-hidden">
              <div className="grid gap-0 md:grid-cols-[220px_1fr]">
                <CardHeader className="border-b border-border bg-secondary/50 md:border-b-0 md:border-r">
                  <Badge variant="secondary" className="w-fit">Step {index + 1}</Badge>
                  <step.icon className="h-6 w-6 text-primary" />
                </CardHeader>
                <CardContent className="p-6">
                  <CardTitle>{step.title}</CardTitle>
                  <p className="mt-3 text-sm leading-7 text-muted-foreground">{step.description}</p>
                </CardContent>
              </div>
            </Card>
          ))}
        </div>
      </MotionSection>
    </>
  );
}
