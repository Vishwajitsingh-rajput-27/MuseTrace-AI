import {
  Accessibility,
  AlertTriangle,
  Bot,
  Brush,
  CheckCircle2,
  Download,
  Eye,
  Github,
  ImageIcon,
  KeyRound,
  Layers3,
  LifeBuoy,
  MousePointerClick,
  Move,
  Palette,
  PanelsTopLeft,
  Route,
  ShieldCheck,
  Sparkles,
  Wand2
} from "lucide-react";
import type { LucideIcon } from "lucide-react";

export const appName = "MuseTrace AI";
export const tagline = "Prompt. Layer. Trace.";
export const downloadUrl = "https://github.com/Vishwajitsingh-rajput-27/MuseTrace-AI/releases/latest";
export const githubUrl = "https://github.com/Vishwajitsingh-rajput-27/MuseTrace-AI";

export const disclaimer =
  "MuseTrace AI is not affiliated with Instagram or Meta. It works only if Instagram Draw is already available on the user's account, app version, region, and device. It does not log into Instagram, use private APIs, scrape, bypass security, or send messages automatically.";

export type NavItem = {
  label: string;
  href: string;
};

export const navItems: NavItem[] = [
  { label: "Home", href: "/" },
  { label: "Features", href: "/features" },
  { label: "How It Works", href: "/how-it-works" },
  { label: "Download", href: "/download" },
  { label: "Setup Guide", href: "/setup-guide" },
  { label: "Gemini Setup", href: "/gemini-setup" },
  { label: "Instagram Draw Guide", href: "/instagram-draw-guide" },
  { label: "Add Space Guide", href: "/add-space-guide" },
  { label: "Floating Overlay Guide", href: "/floating-overlay-guide" },
  { label: "Disclaimer", href: "/disclaimer" },
  { label: "FAQ", href: "/faq" }
];

export type Feature = {
  title: string;
  description: string;
  icon: LucideIcon;
};

export const features: Feature[] = [
  {
    title: "Gemini image generation",
    description: "Turn short prompts into drawing-friendly image ideas with Gemini as the default AI provider.",
    icon: Sparkles
  },
  {
    title: "Gallery and camera import",
    description: "Start from a saved photo, a new camera capture, or a generated image before editing.",
    icon: ImageIcon
  },
  {
    title: "Layered color sketches",
    description: "Convert artwork into 16, 24, or 32-color semi-realistic sketch layers with clear drawing order.",
    icon: Layers3
  },
  {
    title: "Color-first workflow",
    description: "See the current layer color, HEX, RGB, stroke count, and estimated time before each drawing step.",
    icon: Palette
  },
  {
    title: "Floating overlay controller",
    description: "Stay inside Instagram Draw while the overlay guides Continue, pause, resume, skip, and emergency stop.",
    icon: PanelsTopLeft
  },
  {
    title: "Accessibility gesture drawing",
    description: "Uses official Android AccessibilityService gestures only after explicit user confirmation.",
    icon: Accessibility
  },
  {
    title: "Manual calibration",
    description: "Set the real drawing area with top-left, top-right, bottom-left, and bottom-right calibration points.",
    icon: Move
  },
  {
    title: "Privacy-respecting boundaries",
    description: "No Instagram login, no private APIs, no scraping, no automatic messages, and no bypassing restrictions.",
    icon: ShieldCheck
  }
];

export type WorkflowStep = {
  title: string;
  description: string;
  icon: LucideIcon;
};

export const workflowSteps: WorkflowStep[] = [
  {
    title: "Create or import an image",
    description: "Generate with Gemini or import from Gallery or Camera, then edit the image for clean outlines and simple color regions.",
    icon: Wand2
  },
  {
    title: "Build drawing layers",
    description: "MuseTrace AI prepares a limited palette, masks, layer previews, and optimized stroke paths.",
    icon: Layers3
  },
  {
    title: "Open Instagram manually",
    description: "You open Instagram, the target chat, Draw mode, and Add Space if available. The app does not automate those steps.",
    icon: MousePointerClick
  },
  {
    title: "Calibrate the canvas",
    description: "Save the final drawing area so gestures stay inside the selected Instagram Draw canvas.",
    icon: Route
  },
  {
    title: "Trace layer by layer",
    description: "Select each color manually in Instagram Draw, tap Continue in the overlay, and keep Draw open until finished.",
    icon: Brush
  }
];

export type ContentPage = {
  slug: string;
  navLabel: string;
  eyebrow: string;
  title: string;
  description: string;
  icon: LucideIcon;
  sections: Array<{
    title: string;
    body: string;
    tone?: "default" | "warning" | "success";
  }>;
};

export const contentPages: Record<string, ContentPage> = {
  "setup-guide": {
    slug: "setup-guide",
    navLabel: "Setup Guide",
    eyebrow: "Android setup",
    title: "Set up MuseTrace AI on your phone",
    description: "Install the APK, add your Gemini key, enable required Android permissions, and prepare your first drawing project.",
    icon: CheckCircle2,
    sections: [
      {
        title: "Install the APK",
        body: "Download MuseTrace-AI.apk from the latest GitHub release. Open the file on your Android phone and allow installation from that source if Android asks."
      },
      {
        title: "Enter Gemini API key",
        body: "Open Settings in MuseTrace AI, paste your Gemini API key, tap Save key securely, then tap Test Gemini Key. Do not share the key or commit it to GitHub."
      },
      {
        title: "Enable Accessibility",
        body: "Open Android Accessibility settings, choose MuseTrace AI, and turn it on. This permission lets the app draw only after you press Continue in the overlay."
      },
      {
        title: "Enable Display Over Other Apps",
        body: "Allow MuseTrace AI to appear over other apps. This keeps the floating controller visible while Instagram Draw stays open."
      },
      {
        title: "Keep Draw open",
        body: "Do not press Back, Home, switch apps, lock the phone, or close Instagram during drawing. Instagram may clear the current canvas.",
        tone: "warning"
      }
    ]
  },
  "gemini-setup": {
    slug: "gemini-setup",
    navLabel: "Gemini Setup",
    eyebrow: "AI provider",
    title: "Use Gemini safely with MuseTrace AI",
    description: "Gemini is the default image generation provider. The website and Android app never include a hardcoded API key.",
    icon: KeyRound,
    sections: [
      {
        title: "Add your own key",
        body: "Paste your Gemini key in the Android Settings screen. MuseTrace AI stores it with encrypted Android storage and does not print it in logs."
      },
      {
        title: "Test before generating",
        body: "Use Test Gemini Key to confirm the key works. If the test fails, clear the key, paste it again, check your internet connection, and retry."
      },
      {
        title: "Generate drawing-friendly prompts",
        body: "Short ideas are expanded into prompts that favor bold outlines, simple shapes, high contrast, clean color regions, and low background complexity."
      },
      {
        title: "Fallback workflow",
        body: "If generation fails or your quota is limited, import a Gallery or Camera image and continue with the editor."
      }
    ]
  },
  "instagram-draw-guide": {
    slug: "instagram-draw-guide",
    navLabel: "Instagram Draw Guide",
    eyebrow: "Manual Instagram steps",
    title: "Use Instagram Draw manually",
    description: "MuseTrace AI guides drawing gestures, but you control Instagram. You open the app, chat, Draw mode, and color picker yourself.",
    icon: Brush,
    sections: [
      {
        title: "Open Instagram yourself",
        body: "MuseTrace AI does not log into Instagram and never asks for your Instagram username or password."
      },
      {
        title: "Open the target chat",
        body: "Choose the chat manually. The app does not select people, send messages, scrape chats, or use private Instagram APIs."
      },
      {
        title: "Open Draw mode",
        body: "Draw mode must already be available on your account, device, region, and Instagram app version. MuseTrace AI cannot enable it.",
        tone: "warning"
      },
      {
        title: "Select colors manually",
        body: "For each layer, match the color shown in the floating overlay as closely as possible before tapping Continue."
      }
    ]
  },
  "add-space-guide": {
    slug: "add-space-guide",
    navLabel: "Add Space Guide",
    eyebrow: "Canvas size",
    title: "Use Add Space before calibration",
    description: "Add Space is an Instagram feature. MuseTrace AI does not tap it or force it to appear.",
    icon: Move,
    sections: [
      {
        title: "Check if Add Space is available",
        body: "Open Instagram Draw and look for Add Space yourself. Availability depends on Instagram."
      },
      {
        title: "Use it manually first",
        body: "If Add Space is available and you want it, tap it manually before calibration. Then calibrate the final drawing area.",
        tone: "warning"
      },
      {
        title: "Choose a matching profile",
        body: "Use Normal Draw, Add Space Small, Add Space Medium, Add Space Maximum, or Custom calibration profiles so you can switch layouts later."
      },
      {
        title: "Recalibrate after changes",
        body: "If the canvas size changes, recalibrate before starting the overlay session."
      }
    ]
  },
  "floating-overlay-guide": {
    slug: "floating-overlay-guide",
    navLabel: "Floating Overlay Guide",
    eyebrow: "Overlay controller",
    title: "Draw without leaving Instagram",
    description: "The floating overlay keeps layer instructions available while Instagram Draw stays open.",
    icon: PanelsTopLeft,
    sections: [
      {
        title: "Start from Drawing Session",
        body: "Review the checklist, confirm permissions and calibration, then tap Start Overlay Session."
      },
      {
        title: "Read the layer card",
        body: "The overlay shows current layer number, color sample, HEX, RGB, stroke count, estimated time, and progress."
      },
      {
        title: "Tap Continue after selecting color",
        body: "Select the shown color manually in Instagram Draw, then tap Continue in the overlay. MuseTrace AI draws only after that tap."
      },
      {
        title: "Use safety controls",
        body: "Pause, Resume, Skip Layer, Redraw Layer, Cancel, and Emergency Stop are available without switching back to the app."
      },
      {
        title: "Move the overlay if needed",
        body: "Drag it away from the drawing area. If it opens in a bad place, reset overlay position from Settings."
      }
    ]
  },
  disclaimer: {
    slug: "disclaimer",
    navLabel: "Disclaimer",
    eyebrow: "Important limits",
    title: "Independent drawing assistance only",
    description: disclaimer,
    icon: AlertTriangle,
    sections: [
      {
        title: "Not affiliated with Instagram or Meta",
        body: "MuseTrace AI is independent and is not endorsed, sponsored by, or associated with Instagram or Meta.",
        tone: "warning"
      },
      {
        title: "Draw must already be available",
        body: "The app cannot enable, unlock, force, or add Instagram Draw. It only works when Instagram Draw is already available for the user."
      },
      {
        title: "No account automation",
        body: "MuseTrace AI does not log into Instagram, ask for credentials, store Instagram credentials, scrape Instagram, use private APIs, bypass security, or send messages automatically."
      },
      {
        title: "Canvas resume limit",
        body: "Workflow progress can be saved, but MuseTrace AI cannot restore a drawing that Instagram cleared from its canvas.",
        tone: "warning"
      }
    ]
  }
};

export const faqs = [
  {
    question: "Can MuseTrace AI enable Instagram Draw for me?",
    answer: "No. Instagram Draw must already be available on your account, app version, region, and device."
  },
  {
    question: "Does the app need my Instagram login?",
    answer: "No. MuseTrace AI never asks for Instagram credentials and does not log into Instagram."
  },
  {
    question: "Will it send my drawing automatically?",
    answer: "No. You manually send or save anything in Instagram. MuseTrace AI does not send messages."
  },
  {
    question: "Why does the app need Accessibility permission?",
    answer: "It uses official Android AccessibilityService gestures to draw strokes after you tap Continue in the floating overlay."
  },
  {
    question: "Why does it need Display Over Other Apps?",
    answer: "The overlay must stay visible while Instagram Draw stays open, so you do not need to switch back to MuseTrace AI between layers."
  },
  {
    question: "Can I resume if Instagram clears the canvas?",
    answer: "You can resume the MuseTrace project workflow, but MuseTrace AI cannot restore a canvas Instagram deleted. Restart from Layer 1 for best results."
  },
  {
    question: "What if Gemini generation fails?",
    answer: "Check your key, network connection, and rate limits. You can still import an image from Gallery or Camera."
  },
  {
    question: "Where do I download the APK?",
    answer: "Use the latest GitHub release link on this website. The APK filename is MuseTrace-AI.apk."
  }
];

export const quickLinks = [
  { label: "Download latest APK", href: downloadUrl, icon: Download },
  { label: "View GitHub repository", href: githubUrl, icon: Github },
  { label: "Read setup guide", href: "/setup-guide", icon: LifeBuoy },
  { label: "Read disclaimer", href: "/disclaimer", icon: AlertTriangle }
];

export const safetyChecks = [
  "User manually opens Instagram.",
  "User manually opens the chat.",
  "User manually opens Draw mode.",
  "User manually uses Add Space if available.",
  "User manually selects every color.",
  "App draws only after explicit Continue confirmation.",
  "App does not use Instagram private APIs.",
  "App does not send messages automatically."
];
