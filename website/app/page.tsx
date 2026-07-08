import Link from "next/link";
import { AlertTriangle, ArrowRight, Download, Github } from "lucide-react";
import { HeroScene } from "@/components/hero-scene";
import { MotionSection } from "@/components/motion-section";
import { SectionHeading } from "@/components/section-heading";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { disclaimer, downloadUrl, faqs, features, githubUrl, safetyChecks, workflowSteps } from "@/lib/site";

export default function HomePage() {
  return (
    <>
      <HeroScene />
      <section className="container -mt-24 flex flex-wrap gap-3 pb-16">
        <Button asChild size="lg">
          <Link href={downloadUrl}>
            <Download className="h-4 w-4" />
            Download APK
          </Link>
        </Button>
        <Button asChild variant="outline" size="lg">
          <Link href={githubUrl}>
            <Github className="h-4 w-4" />
            GitHub
          </Link>
        </Button>
      </section>

      <MotionSection className="container py-16">
        <SectionHeading
          eyebrow="Features"
          title="Built for layered Android drawing"
          description="MuseTrace AI keeps the creative workflow inside Android while respecting Instagram safety boundaries."
        />
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {features.slice(0, 8).map((feature) => (
            <Card key={feature.title} className="bg-card/70">
              <CardHeader>
                <feature.icon className="h-5 w-5 text-primary" />
                <CardTitle className="text-base">{feature.title}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm leading-6 text-muted-foreground">{feature.description}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </MotionSection>

      <MotionSection className="border-y border-border bg-card/30 py-16">
        <div className="container">
          <SectionHeading
            eyebrow="Workflow"
            title="From prompt to traced layers"
            description="The app prepares the artwork, then the floating overlay guides each color while you stay in Instagram Draw."
          />
          <div className="grid gap-4 lg:grid-cols-5">
            {workflowSteps.map((step, index) => (
              <Card key={step.title}>
                <CardHeader>
                  <Badge variant="secondary" className="w-fit">Step {index + 1}</Badge>
                  <step.icon className="h-5 w-5 text-primary" />
                  <CardTitle className="text-base">{step.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm leading-6 text-muted-foreground">{step.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </MotionSection>

      <MotionSection className="container grid gap-4 py-16 lg:grid-cols-[1fr_0.8fr]">
        <Card className="border-amber-500/35 bg-amber-500/10">
          <CardHeader>
            <Badge variant="warning" className="w-fit gap-2">
              <AlertTriangle className="h-3.5 w-3.5" />
              Disclaimer
            </Badge>
            <CardTitle>Independent drawing assistance only</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm leading-7 text-muted-foreground">{disclaimer}</p>
            <Button asChild variant="outline" className="mt-6">
              <Link href="/disclaimer">
                Read full disclaimer
                <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Safety boundaries</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {safetyChecks.map((item) => (
              <div key={item} className="flex gap-3 text-sm text-muted-foreground">
                <span className="mt-2 h-1.5 w-1.5 rounded-full bg-primary" />
                <span>{item}</span>
              </div>
            ))}
          </CardContent>
        </Card>
      </MotionSection>

      <MotionSection className="container pb-20">
        <SectionHeading eyebrow="FAQ" title="Common questions" description="Short answers for normal Android users setting up MuseTrace AI." />
        <Card className="mx-auto max-w-3xl">
          <CardContent className="p-6">
            <Accordion type="single" collapsible>
              {faqs.slice(0, 6).map((faq, index) => (
                <AccordionItem key={faq.question} value={`item-${index}`}>
                  <AccordionTrigger>{faq.question}</AccordionTrigger>
                  <AccordionContent>{faq.answer}</AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          </CardContent>
        </Card>
      </MotionSection>
    </>
  );
}
