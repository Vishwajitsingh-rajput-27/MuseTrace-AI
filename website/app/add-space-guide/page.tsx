import type { Metadata } from "next";
import { ContentPageView } from "@/components/content-page";
import { contentPages } from "@/lib/site";

export const metadata: Metadata = {
  title: "Add Space Guide"
};

export default function AddSpaceGuidePage() {
  return <ContentPageView page={contentPages["add-space-guide"]} />;
}
