import type { LucideIcon } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { MotionSection } from "@/components/motion-section";

type PageHeroProps = {
  eyebrow: string;
  title: string;
  description: string;
  icon: LucideIcon;
};

export function PageHero({ eyebrow, title, description, icon: Icon }: PageHeroProps) {
  return (
    <MotionSection className="container py-14 md:py-20">
      <Badge variant="outline" className="gap-2">
        <Icon className="h-3.5 w-3.5 text-primary" />
        {eyebrow}
      </Badge>
      <h1 className="mt-6 max-w-3xl text-4xl font-semibold tracking-normal md:text-6xl">{title}</h1>
      <p className="mt-5 max-w-2xl text-base leading-8 text-muted-foreground md:text-lg">{description}</p>
    </MotionSection>
  );
}
