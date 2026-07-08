import { AlertTriangle, CheckCircle2 } from "lucide-react";
import { MotionSection } from "@/components/motion-section";
import { PageHero } from "@/components/page-hero";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { ContentPage } from "@/lib/site";

export function ContentPageView({ page }: { page: ContentPage }) {
  return (
    <>
      <PageHero eyebrow={page.eyebrow} title={page.title} description={page.description} icon={page.icon} />
      <MotionSection className="container grid gap-4 pb-16 md:grid-cols-2">
        {page.sections.map((section) => {
          const warning = section.tone === "warning";
          const success = section.tone === "success";
          const Icon = warning ? AlertTriangle : CheckCircle2;
          return (
            <Card key={section.title} className={warning ? "border-amber-500/35 bg-amber-500/10" : ""}>
              <CardHeader>
                <Badge variant={warning ? "warning" : success ? "default" : "outline"} className="w-fit gap-2">
                  <Icon className="h-3.5 w-3.5" />
                  {warning ? "Important" : "Step"}
                </Badge>
                <CardTitle>{section.title}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm leading-7 text-muted-foreground">{section.body}</p>
              </CardContent>
            </Card>
          );
        })}
      </MotionSection>
    </>
  );
}
