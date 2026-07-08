import type { Metadata } from "next";
import { ContentPageView } from "@/components/content-page";
import { contentPages } from "@/lib/site";

export const metadata: Metadata = {
  title: "Instagram Draw Guide"
};

export default function InstagramDrawGuidePage() {
  return <ContentPageView page={contentPages["instagram-draw-guide"]} />;
}
