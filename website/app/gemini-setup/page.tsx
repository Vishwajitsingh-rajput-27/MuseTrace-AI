import type { Metadata } from "next";
import { ContentPageView } from "@/components/content-page";
import { contentPages } from "@/lib/site";

export const metadata: Metadata = {
  title: "Gemini Setup"
};

export default function GeminiSetupPage() {
  return <ContentPageView page={contentPages["gemini-setup"]} />;
}
