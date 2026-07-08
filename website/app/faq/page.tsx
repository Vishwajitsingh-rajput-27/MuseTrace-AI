import type { Metadata } from "next";
import { HelpCircle } from "lucide-react";
import { MotionSection } from "@/components/motion-section";
import { PageHero } from "@/components/page-hero";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";
import { Card, CardContent } from "@/components/ui/card";
import { faqs } from "@/lib/site";

export const metadata: Metadata = {
  title: "FAQ"
};

export default function FaqPage() {
  return (
    <>
      <PageHero
        eyebrow="FAQ"
        title="Questions before you draw"
        description="Clear answers about permissions, Gemini, Instagram limits, overlay drawing, and resume behavior."
        icon={HelpCircle}
      />
      <MotionSection className="container pb-16">
        <Card className="mx-auto max-w-3xl">
          <CardContent className="p-6">
            <Accordion type="single" collapsible>
              {faqs.map((faq, index) => (
                <AccordionItem key={faq.question} value={`faq-${index}`}>
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
