/**
 * Cloud Functions Testleri — İçerik Filtresi
 *
 * CLAUDE.md §32:
 * - Filtre aşma girişimleri testi
 * - Kişisel veri tespiti testi
 *
 * Bu testler Firebase Emulator gerektirmez — saf TypeScript birim testleridir.
 */

// functions/src kopyası — path alias kullanmak yerine doğrudan import
// Gerçek import için functions klasörüne symlink veya tsconfig paths gerekebilir.
// Test ortamında dosyayı kopyalayarak ya da relative import ile kullanın.
import { analyzeContent, FilterResult } from "../../functions/src/moderation/contentFilter";

describe("analyzeContent — temel kontrol", () => {
  test("Temiz içerik NONE döndürür", () => {
    const result = analyzeContent("Ev sahibi çok ilgiliydi, depozito iade edildi.", []);
    expect(result.riskLevel).toBe("NONE");
    expect(result.blocked).toBe(false);
    expect(result.flagged).toBe(false);
  });

  test("Yasaklı kelime içeren içerik HIGH döndürür", () => {
    const result = analyzeContent("Bu ev sahibi çok kötü", ["kötü"]);
    expect(result.riskLevel).toBe("HIGH");
    expect(result.blocked).toBe(true);
  });
});

describe("analyzeContent — Türkçe karakter bypass", () => {
  test("Türkçe karakterler olmadan yazılan kelime tespit edilir", () => {
    // "kötü" → "kotu" normalize edilir; bannedTerms'te de normalize edilir
    const result = analyzeContent("Bu ev sahibi cok kotu", ["kötü"]);
    expect(result.blocked).toBe(true);
  });

  test("Büyük harf bypass tespit edilir", () => {
    const result = analyzeContent("Bu ev sahibi COK KOTU", ["kötü"]);
    expect(result.blocked).toBe(true);
  });
});

describe("analyzeContent — Leet speak bypass", () => {
  test("Rakamla yazılan harf bypass tespit edilir", () => {
    // "k0tu" → normalize → "kotu" eşleşir "kötü"
    const result = analyzeContent("Bu adam k0tu biri", ["kötü"]);
    expect(result.blocked).toBe(true);
  });

  test("@ sembolü ile bypass tespit edilir", () => {
    // "@dam" → normalize → "adam"
    const result = analyzeContent("Bu @d@m çok kötü", ["kötü"]);
    expect(result.blocked).toBe(true);
  });
});

describe("analyzeContent — Harf tekrarı bypass", () => {
  test("Tekrar eden harfler normalize edilir", () => {
    // "köttttü" → normalize → tekrar → "kotu"
    const result = analyzeContent("Bu ev sahibi cokkkk kotuuu", ["kötü"]);
    expect(result.blocked).toBe(true);
  });
});

describe("analyzeContent — Kişisel veri tespiti", () => {
  test("Telefon numarası MEDIUM risk verir", () => {
    const result = analyzeContent(
      "Beni aramak isterseniz 0532 123 45 67 numaramdan ulaşabilirsiniz.",
      []
    );
    expect(result.reasons.some((r) => r.includes("Telefon"))).toBe(true);
    // HIGH veya üstü olmalı
    const riskOrder = ["NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL"];
    expect(riskOrder.indexOf(result.riskLevel)).toBeGreaterThanOrEqual(
      riskOrder.indexOf("HIGH")
    );
  });

  test("IBAN tespiti CRITICAL risk verir", () => {
    const result = analyzeContent(
      "Ödemeyi TR33 0006 1005 1978 6457 8413 26 numaralı hesabıma yapın.",
      []
    );
    expect(result.reasons.some((r) => r.includes("IBAN"))).toBe(true);
    expect(result.riskLevel).toBe("CRITICAL");
    expect(result.blocked).toBe(true);
  });

  test("E-posta adresi MEDIUM risk verir", () => {
    const result = analyzeContent(
      "Bana test@example.com adresinden ulaşabilirsiniz.",
      []
    );
    expect(result.reasons.some((r) => r.includes("posta"))).toBe(true);
    const riskOrder = ["NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL"];
    expect(riskOrder.indexOf(result.riskLevel)).toBeGreaterThanOrEqual(
      riskOrder.indexOf("MEDIUM")
    );
  });

  test("Adres bilgisi MEDIUM risk verir", () => {
    const result = analyzeContent(
      "Atatürk Mahalle Gül Sokak No:5 Daire 3 adresinde oturuyorum.",
      []
    );
    expect(result.flagged).toBe(true);
  });
});

describe("analyzeContent — Spam tespiti", () => {
  test("Aşırı büyük harf LOW risk verir", () => {
    const result = analyzeContent(
      "BU EV SAHİBİ ÇOK KÖTÜ MUAMELE YAPIYOR HERKESE SÖYLÜYORUM",
      []
    );
    const riskOrder = ["NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL"];
    expect(riskOrder.indexOf(result.riskLevel)).toBeGreaterThanOrEqual(
      riskOrder.indexOf("LOW")
    );
  });

  test("Çok sayıda URL MEDIUM risk verir", () => {
    const result = analyzeContent(
      "Ziyaret edin: https://link1.com ve https://link2.com ve https://link3.com",
      []
    );
    expect(result.reasons.some((r) => r.includes("URL"))).toBe(true);
  });
});

describe("analyzeContent — Temiz içerik geçer", () => {
  test("Normal kiracı şikayeti geçer", () => {
    const result = analyzeContent(
      "Ev sahibim 3 aydır tıkanan lavaboyu onarmadı. Ne yapmam gerekiyor?",
      ["kötüfür", "hakaret"]
    );
    expect(result.blocked).toBe(false);
    expect(result.riskLevel).toBe("NONE");
  });

  test("Normal referans metni geçer", () => {
    const result = analyzeContent(
      "Kiracım 2 yıl boyunca düzenli kira ödedi ve evi temiz kullandı.",
      []
    );
    expect(result.blocked).toBe(false);
  });
});