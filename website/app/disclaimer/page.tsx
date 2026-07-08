import type { Metadata } from "next";
import { ContentPageView } from "@/components/content-page";
import { contentPages } from "@/lib/site";

export const metadata: Metadata = {
  title: "Disclaimer"
};

export default function DisclaimerPage() {
  return <ContentPageView page={contentPages.disclaimer} />;
}
