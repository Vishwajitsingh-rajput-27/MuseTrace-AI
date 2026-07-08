import type { Metadata } from "next";
import { ContentPageView } from "@/components/content-page";
import { contentPages } from "@/lib/site";

export const metadata: Metadata = {
  title: "Setup Guide"
};

export default function SetupGuidePage() {
  return <ContentPageView page={contentPages["setup-guide"]} />;
}
