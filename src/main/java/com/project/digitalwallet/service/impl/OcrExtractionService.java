package com.project.digitalwallet.service.impl;

import com.project.digitalwallet.common.enums.DocumentType;
import com.project.digitalwallet.dto.SubmitKycRequest;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrExtractionService {

    @Value("${tesseract.datapath}")
    private String datapath;

    @Value("${tesseract.language:eng+nep}")
    private String language;

    public SubmitKycRequest extractKycDetails(MultipartFile file) {
        String rawText = performOcr(file);

        // Normalize Devanagari digits, symbols, and OCR character confusions
        String normalizedText = normalizeText(rawText);

        SubmitKycRequest extractedData = new SubmitKycRequest();

        // 1. Detect Document Type
        extractedData.setDocumentType(parseDocumentType(rawText));

        // 2. Extract Document Number (000-000-0000 format)
        extractedData.setDocumentNumber(parseDocumentNumber(normalizedText));

        // 3. Extract and map Dates (DOB and Issue Date)
        extractAndSetDates(normalizedText, extractedData);

        return extractedData;
    }

    private DocumentType parseDocumentType(String text) {
        if (text == null || text.isBlank()) return null;

        String upperText = text.toUpperCase();

        if (upperText.contains("NATIONAL IDENTITY CARD") || upperText.contains("NATIONAL ID")) {
            return DocumentType.NATIONAL_ID;
        }
//        } else if (upperText.contains("CITIZENSHIP")) {
//            return DocumentType.CITIZENSHIP;
//        }

        return null;
    }

    private String normalizeText(String text) {
        if (text == null) return null;

        return text
                // 1. Convert Devanagari digits to standard ASCII digits
                .replace('०', '0')
                .replace('१', '1')
                .replace('२', '2')
                .replace('३', '3')
                .replace('४', '4')
                .replace('५', '5')
                .replace('६', '6')
                .replace('७', '7')
                .replace('८', '8')
                .replace('९', '9')
                // 2. Fix common OCR character confusion in dates/numbers (e.g., 'o' or 'O' surrounded by numbers -> '0')
                .replaceAll("(?<=\\d)[oO]|[oO](?=\\d)", "0");
    }

    private String performOcr(MultipartFile file) {
        File tempFile = null;
        try {
            // Create temporary file on disk for Tesseract native library to read
            tempFile = File.createTempFile("ocr_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(datapath);
            tesseract.setLanguage(language);

            String rawText = tesseract.doOCR(tempFile);
//
//            System.out.println("==================== RAW OCR TEXT START ====================");
//            System.out.println(rawText);
//            System.out.println("====================  RAW OCR TEXT END  ====================");
//
            return rawText;

        } catch (Exception e) {
            throw new RuntimeException("Failed to perform OCR on identity document: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete(); // Cleanup temp file
            }
        }
    }

    private String parseDocumentNumber(String text) {
        if (text == null || text.isBlank()) return null;

        // Matches 3 digits, separator, 3 digits, separator, 4 digits
        // Separators include: hyphen (-), colon (:), semicolon (;), dot (.), space ( ), en-dash (–), em-dash (—)
        Pattern pattern = Pattern.compile("(?<!\\d)(\\d{3})[\\s.\\-–—:;]*(\\d{3})[\\s.\\-–—:;]*(\\d{4})(?!\\d)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            // Reconstruct cleanly as 000-000-0000 using standard hyphens
            return String.format("%s-%s-%s",
                    matcher.group(1),
                    matcher.group(2),
                    matcher.group(3));
        }
        return null;
    }

    private void extractAndSetDates(String normalizedText, SubmitKycRequest request) {
        if (normalizedText == null || normalizedText.isBlank()) return;

        List<LocalDate> validAdDates = new ArrayList<>();

        // Regex for YYYY-MM-DD, YYYY/MM/DD, DD-MM-YYYY, etc.
        Pattern pattern = Pattern.compile("\\b(\\d{4}[-/.]\\d{2}[-/.]\\d{2}|\\d{2}[-/.]\\d{2}[-/.]\\d{4})\\b");
        Matcher matcher = pattern.matcher(normalizedText);

        while (matcher.find()) {
            String dateStr = matcher.group().replaceAll("[./]", "-");
            try {
                LocalDate parsedDate;
                if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    parsedDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                } else {
                    parsedDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                }

                // Filter AD calendar years (e.g., 1950 to 2030) to exclude Bikram Sambat (B.S.) years like 2080
                if (parsedDate.getYear() >= 1950 && parsedDate.getYear() <= 2030) {
                    validAdDates.add(parsedDate);
                }
            } catch (Exception ignored) {}
        }

        if (validAdDates.isEmpty()) return;

        // Sort dates chronologically
        Collections.sort(validAdDates);

        if (validAdDates.size() >= 2) {
            // Earliest date is Date of Birth, latest date is Issue Date
            request.setDateOfBirth(validAdDates.get(0));
            request.setIssueDate(validAdDates.get(validAdDates.size() - 1));
        } else {
            // If only 1 valid A.D. date found, check context or assign to DOB
            request.setDateOfBirth(validAdDates.get(0));
        }
    }
}