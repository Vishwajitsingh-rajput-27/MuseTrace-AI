import Link from "next/link";
import type { Metadata } from "next";
import { Download, Github, ShieldCheck } from "lucide-react";
import { MotionSection } from "@/components/motion-section";
import { PageHero } from "@/components/page-hero";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { downloadUrl, githubUrl } from "@/lib/site";

export const metadata: Metadata = {
  title: "Download"
};

export default function DownloadPage() {
  return (
    <>
      <PageHero
        eyebrow="Download"
        title="Get the latest MuseTrace AI APK"
        description="Download the Android APK from the official GitHub release page, then install it manually on your phone."
        icon={Download}
      />
      <MotionSection className="container grid gap-4 pb-16 lg:grid-cols-[1fr_0.8fr]">
        <Card>
          <CardHeader>
            <CardTitle>Latest release</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm leading-7 text-muted-foreground">
              The direct release link always points to the newest GitHub release. The APK filename is MuseTrace-AI.apk.
            </p>
            <div className="mt-6 flex flex-wrap gap-3">
              <Button asChild size="lg">
                <Link href={downloadUrl}>
                  <Download className="h-4 w-4" />
                  Download MuseTrace-AI.apk
                </Link>
              </Button>
              <Button asChild variant="outline" size="lg">
                <Link href={githubUrl}>
                  <Github className="h-4 w-4" />
                  Open GitHub
                </Link>
              </Button>
            </div>
          </CardContent>
        </Card>
        <Card className="border-primary/30">
          <CardHeader>
            <ShieldCheck className="h-5 w-5 text-primary" />
            <CardTitle>Install safely</CardTitle>
          </CardHeader>
          <CardContent>
            <ol className="space-y-3 text-sm leading-7 text-muted-foreground">
              <li>1. Download the APK from GitHub releases.</li>
              <li>2. Open it on your Android phone.</li>
              <li>3. Allow installs from that source if Android asks.</li>
              <li>4. Open MuseTrace AI and read the first-launch disclaimer.</li>
              <li>5. Configure Gemini, Accessibility, overlay permission, and calibration.</li>
            </ol>
          </CardContent>
        </Card>
      </MotionSection>
    </>
  );
}
