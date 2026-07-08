import type { Metadata } from "next";
import { ContentPageView } from "@/components/content-page";
import { contentPages } from "@/lib/site";

export const metadata: Metadata = {
  title: "Floating Overlay Guide"
};

export default function FloatingOverlayGuidePage() {
  return <ContentPageView page={contentPages["floating-overlay-guide"]} />;
}
