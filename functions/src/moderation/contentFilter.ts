/**
 * Mülkora İçerik Filtresi — Temel Analiz Motoru
 *
 * CLAUDE.md §12: Küfür, Argo ve Zararlı İçerik Filtresi
 *
 * Algılanan bypass teknikleri:
 * - Tam kelime eşleşmesi
 * - Büyük/küçük harf değişiklikleri (normalize edilir)
 * - Türkçe karakter değişiklikleri (ş→s, ı→i, ğ→g, ü→u, ö→o, ç→c)
 * - Harf yerine rakam kullanımı (a→4, e→3, i→1, o→0, s→5, ...)
 * - Kelimelerin boşluk/nokta ile bölünmesi
 * - Harf tekrarları (küüüüfür → küfür)
 * - Sembollerle filtre aşma (k@fir → kafir)
 * - Benzer yazılışlar
 *
 * Kişisel veri tespiti:
 * - Telefon numarası (Türkiye formatları)
 * - T.C. Kimlik Numarası
 * - Açık adres (sokak, mahalle, daire vb.)
 * - IBAN ve banka bilgileri
 * - E-posta adresi
 *
 * Risk seviyeleri:
 * - LOW: Uyarı göster
 * - MEDIUM: Moderasyon kuyruğuna gönder
 * - HIGH: Gönderimi engelle
 * - CRITICAL: İçeriği engelle + yöneticiyi bilgilendir
 */

export type RiskLevel = "NONE" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface FilterResult {
  riskLevel: RiskLevel;
  reasons: string[];
  flagged: boolean;
  blocked: boolean;
}

// ─── Normalizasyon ──────────────────────────────────────────────────────────

/**
 * Metni tüm bypass tekniklerini kaldırarak normalize eder.
 * Filtreleme bu normalize edilmiş metin üzerinden yapılır.
 */
export function normalize(text: string): string {
  let t = text.toLowerCase();

  // 1. Türkçe karakter dönüşümü
  t = t
    .replace(/ş/g, "s")
    .replace(/ı/g, "i")
    .replace(/ğ/g, "g")
    .replace(/ü/g, "u")
    .replace(/ö/g, "o")
    .replace(/ç/g, "c")
    .replace(/â/g, "a")
    .replace(/î/g, "i")
    .replace(/û/g, "u");

  // 2. Harf benzeri rakam ve sembol dönüşümü (leet speak)
  t = t
    .replace(/0/g, "o")
    .replace(/1/g, "i")
    .replace(/3/g, "e")
    .replace(/4/g, "a")
    .replace(/5/g, "s")
    .replace(/7/g, "t")
    .replace(/8/g, "b")
    .replace(/@/g, "a")
    .replace(/\$/g, "s")
    .replace(/\+/g, "t")
    .replace(/!/g, "i")
    .replace(/\|/g, "l");

  // 3. Tekrar eden harfleri teke indir (küüüüfür → kufur)
  t = t.replace(/(.)\1{2,}/g, "$1");

  // 4. Noktalama, boşluk ve özel karakterleri kaldır
  t = t.replace(/[\s\-_.,:;/\\*#%^&()[\]{}'"<>~`]/g, "");

  return t;
}

// ─── Kişisel Veri Desenleri ─────────────────────────────────────────────────

const PERSONAL_DATA_PATTERNS: Array<{ name: string; pattern: RegExp; risk: RiskLevel }> = [
  {
    name: "Telefon numarası",
    // Türkiye: 05xx xxx xxxx, +90 5xx, 0(5xx), vb.
    pattern:
      /(?:\+90|0090|0)\s*[\s\-.]?\s*(?:5\d{2})\s*[\s\-.]?\s*\d{3}\s*[\s\-.]?\s*\d{2}\s*[\s\-.]?\s*\d{2}/g,
    risk: "HIGH",
  },
  {
    name: "T.C. Kimlik Numarası",
    // 11 haneli, ilk hane sıfır olamaz
    pattern: /\b[1-9]\d{10}\b/g,
    risk: "CRITICAL",
  },
  {
    name: "IBAN",
    // TR + 24 rakam
    pattern: /\bTR\s*\d{2}\s*(?:\d{4}\s*){5}\d{2}\b/gi,
    risk: "CRITICAL",
  },
  {
    name: "E-posta adresi",
    pattern: /\b[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}\b/g,
    risk: "MEDIUM",
  },
  {
    name: "Banka hesap numarası",
    // 16–26 ardışık rakam (kart/IBAN benzeri ama TR ile başlamayan)
    pattern: /\b\d[\d\s\-]{14,24}\d\b/g,
    risk: "HIGH",
  },
  {
    name: "Açık adres",
    // Türkçe adres anahtar kelimeleri
    pattern:
      /\b(?:sokak|sok\.|cadde|cad\.|bulvar|blv\.|mahalle|mah\.|daire|kat\s*\d+|no\s*:\s*\d+|apartman|site\s+\w+)\b/gi,
    risk: "MEDIUM",
  },
];

// ─── Tehdit İfadeleri ───────────────────────────────────────────────────────

/**
 * Normalize edilmiş metin üzerinde aranacak tehdit kalıpları.
 * Her kalıp normalize() çıktısıyla karşılaştırılır.
 */
const THREAT_PATTERNS: Array<{ pattern: RegExp; risk: RiskLevel }> = [
  // Fiziksel tehdit
  { pattern: /oldururum|oldiriyim|bicirim|bicaklarim|yakacagim|yakarim/i, risk: "CRITICAL" },
  // Doxxing
  { pattern: /adresinibiliyorum|neredeoturdugunubiliyorum/i, risk: "CRITICAL" },
  // Ölüm tehdidi
  { pattern: /olumtehdidi|oldurecegim|caninalacagim/i, risk: "CRITICAL" },
];

// ─── Yasaklı Kelime Eşleşmesi ──────────────────────────────────────────────

/**
 * Firestore'dan çekilen yasaklı kelimeleri normalize edilmiş metinle karşılaştırır.
 * Her kelime de normalize edilerek bypass önlenir.
 */
function matchesBannedTerms(
  normalizedText: string,
  bannedTerms: string[]
): string[] {
  const matched: string[] = [];
  for (const term of bannedTerms) {
    const normalizedTerm = normalize(term);
    if (normalizedTerm && normalizedText.includes(normalizedTerm)) {
      matched.push(term);
    }
  }
  return matched;
}

// ─── Ayrımcı / Nefret Söylemi Kalıpları ────────────────────────────────────

const HATE_SPEECH_PATTERNS: RegExp[] = [
  /irkci|irkcilk|nsist|racial/i,
  // Belirli grupları hedef alan kalıplar burada genişletilebilir
];

// ─── Ana Filtre Fonksiyonu ──────────────────────────────────────────────────

/**
 * Verilen metni tüm kontrol katmanlarından geçirir ve risk sonucu döndürür.
 *
 * @param text Filtrelenecek ham metin (başlık + içerik)
 * @param bannedTerms Firestore'dan alınan yasaklı kelimeler listesi
 */
export function analyzeContent(
  text: string,
  bannedTerms: string[]
): FilterResult {
  const reasons: string[] = [];
  let maxRisk: RiskLevel = "NONE";

  const riskOrder: RiskLevel[] = ["NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL"];
  const escalate = (level: RiskLevel) => {
    if (riskOrder.indexOf(level) > riskOrder.indexOf(maxRisk)) {
      maxRisk = level;
    }
  };

  const normalizedText = normalize(text);

  // 1. Yasaklı kelime kontrolü (Firestore listesi)
  const matchedBanned = matchesBannedTerms(normalizedText, bannedTerms);
  if (matchedBanned.length > 0) {
    reasons.push(`Yasaklı kelime içeriyor (${matchedBanned.length} eşleşme)`);
    escalate("HIGH");
  }

  // 2. Kişisel veri tespiti (orijinal metinde — normalizasyon rakamları değiştirdiğinden)
  for (const { name, pattern, risk } of PERSONAL_DATA_PATTERNS) {
    pattern.lastIndex = 0; // global flag sıfırla
    if (pattern.test(text)) {
      reasons.push(`Kişisel veri içeriyor: ${name}`);
      escalate(risk);
    }
  }

  // 3. Tehdit ifadesi tespiti (normalize edilmiş)
  for (const { pattern, risk } of THREAT_PATTERNS) {
    if (pattern.test(normalizedText)) {
      reasons.push("Tehdit ifadesi tespit edildi");
      escalate(risk);
      break;
    }
  }

  // 4. Nefret söylemi / ayrımcılık
  for (const pattern of HATE_SPEECH_PATTERNS) {
    if (pattern.test(normalizedText)) {
      reasons.push("Ayrımcı veya nefret söylemi içeriyor");
      escalate("HIGH");
      break;
    }
  }

  // 5. Aşırı büyük harf kullanımı (spam göstergesi)
  const upperRatio =
    (text.match(/[A-ZÜÖÇŞĞİ]/g) || []).length / Math.max(text.length, 1);
  if (upperRatio > 0.6 && text.length > 10) {
    reasons.push("Aşırı büyük harf kullanımı (spam göstergesi)");
    escalate("LOW");
  }

  // 6. URL spam kontrolü
  const urlCount = (text.match(/https?:\/\/\S+/g) || []).length;
  if (urlCount > 2) {
    reasons.push("Çok sayıda URL içeriyor");
    escalate("MEDIUM");
  }

  return {
    riskLevel: maxRisk,
    reasons,
    flagged: riskOrder.indexOf(maxRisk) >= riskOrder.indexOf("MEDIUM"),
    blocked: riskOrder.indexOf(maxRisk) >= riskOrder.indexOf("HIGH"),
  };
}

// ─── Async Wrapper (addComment / editComment uyumu) ─────────────────────────

import * as admin from "firebase-admin";
import { loadBannedTerms } from "./bannedTermsLoader";

export interface ContentFilterResult {
  riskLevel: RiskLevel;
  reasons: string[];
  matchedTerms: string[];
  flagged: boolean;
  blocked: boolean;
}

/**
 * Async wrapper — Firestore'dan bannedTerms yükleyip `analyzeContent` çağırır.
 *
 * addComment, editComment gibi callable fonksiyonlar bu API'yi kullanır.
 * Ortak yükleme yardımcısı (bannedTermsLoader) 1000 limitiyle tutarlı liste sağlar.
 */
export async function checkContentFilter(
  text: string
): Promise<ContentFilterResult> {
  const db = admin.firestore();
  const bannedTerms = await loadBannedTerms(db);
  const result = analyzeContent(text, bannedTerms);

  // matchedTerms: addComment moderasyon kuyruğu kaydı için
  const normalizedText = normalize(text);
  const matchedTerms = bannedTerms.filter((term) => {
    const n = normalize(term);
    return n && normalizedText.includes(n);
  });

  return { ...result, matchedTerms };
}