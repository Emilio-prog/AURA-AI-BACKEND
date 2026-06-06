package com.auraia.backend.services.user;

import com.auraia.backend.models.dto.response.DomainResponses;
import com.auraia.backend.models.dto.response.UserResponses;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class UserExportPdfService {

    private static final PDFont TITLE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float MARGIN = 48;
    private static final float LEADING = 15;

    public byte[] render(UserResponses.ExportDataResponse export) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document);
            writer.title("AURA IA - Exportacion de datos");
            writer.line("Generado: " + export.getExportedAt());
            writer.blank();
            profile(writer, export);
            diary(writer, export);
            moods(writer, export);
            chats(writer, export);
            contacts(writer, export);
            panic(writer, export);
            writer.close();
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to render user export PDF", ex);
        }
    }

    private void profile(PdfWriter writer, UserResponses.ExportDataResponse export) throws IOException {
        writer.section("Perfil");
        writer.line("Nombre: " + export.getProfile().getName());
        writer.line("Email: " + export.getProfile().getEmail());
        writer.line("Plan: " + export.getProfile().getPlan());
        writer.line("Email verificado: " + export.getProfile().isEmailVerified());
        if (export.getSettings() != null) {
            writer.line("Idioma: " + export.getSettings().getLanguage());
            writer.line("Zona horaria: " + export.getSettings().getTimezone());
            writer.line("Preferencias de notificacion: " + export.getSettings().getNotificationPreferences());
        }
        writer.blank();
    }

    private void diary(PdfWriter writer, UserResponses.ExportDataResponse export) throws IOException {
        writer.section("Diario");
        if (export.getDiary().isEmpty()) {
            writer.line("Sin entradas.");
        }
        for (DomainResponses.DiaryEntryResponse entry : export.getDiary()) {
            writer.line("[" + entry.getCreatedAt() + "] " + fallback(entry.getTitle(), "Entrada"));
            writer.line("Mood: " + fallback(entry.getMoodLabel(), "-") + " / " + fallback(entry.getMoodScore(), "-"));
            writer.line(entry.getContent());
            writer.line("Tags: " + entry.getTags());
            writer.blank();
        }
    }

    private void moods(PdfWriter writer, UserResponses.ExportDataResponse export) throws IOException {
        writer.section("Mood");
        if (export.getMoods().isEmpty()) {
            writer.line("Sin registros.");
        }
        for (DomainResponses.MoodLogResponse mood : export.getMoods()) {
            writer.line("[" + mood.getLoggedAt() + "] antes=" + mood.getBeforeLevel() + " despues=" + mood.getAfterLevel());
            writer.line("Nota: " + fallback(mood.getNote(), "-"));
        }
        writer.blank();
    }

    private void chats(PdfWriter writer, UserResponses.ExportDataResponse export) throws IOException {
        writer.section("Chat");
        if (export.getChatSessions().isEmpty()) {
            writer.line("Sin sesiones.");
        }
        for (DomainResponses.ChatSessionResponse session : export.getChatSessions()) {
            writer.line("Sesion: " + fallback(session.getTitle(), session.getId()) + " [" + session.getStartedAt() + "]");
            for (Map<String, Object> message : session.getMessages()) {
                writer.line(fallback(message.get("role"), "mensaje") + ": " + fallback(message.get("content"), ""));
            }
            writer.blank();
        }
    }

    private void contacts(PdfWriter writer, UserResponses.ExportDataResponse export) throws IOException {
        writer.section("Contactos");
        if (export.getContacts().isEmpty()) {
            writer.line("Sin contactos.");
        }
        for (DomainResponses.ContactResponse contact : export.getContacts()) {
            writer.line(contact.getName() + " - " + fallback(contact.getRelationship(), "-") + " - " + contact.getPhone());
            writer.line("Disponible: " + contact.isAvailable() + " / SOS: " + contact.isSosEnabled());
        }
        writer.blank();
    }

    private void panic(PdfWriter writer, UserResponses.ExportDataResponse export) throws IOException {
        writer.section("SOS");
        if (export.getPanicAlerts().isEmpty()) {
            writer.line("Sin alertas SOS.");
        }
        for (DomainResponses.PanicAlertResponse alert : export.getPanicAlerts()) {
            writer.line("Alerta: " + alert.getTriggeredAt() + " / resuelta: " + fallback(alert.getResolvedAt(), "-"));
            writer.line("Notas: " + fallback(alert.getNotes(), "-"));
            for (DomainResponses.PanicNotificationResponse notification : alert.getNotifications()) {
                writer.line(notification.getChannel() + " " + notification.getStatus() + " -> " + notification.getContactName());
            }
            writer.blank();
        }
    }

    private String fallback(Object value, Object fallback) {
        return value == null || value.toString().isBlank() ? String.valueOf(fallback) : value.toString();
    }

    private static class PdfWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        PdfWriter(PDDocument document) throws IOException {
            this.document = document;
            newPage();
        }

        void title(String text) throws IOException {
            write(text, TITLE, 18);
        }

        void section(String text) throws IOException {
            blank();
            write(text, BOLD, 13);
        }

        void line(String text) throws IOException {
            for (String part : wrap(text, 92)) {
                write(part, REGULAR, 10);
            }
        }

        void blank() throws IOException {
            ensureSpace();
            y -= LEADING;
        }

        void close() throws IOException {
            if (content != null) {
                content.close();
            }
        }

        private void write(String text, PDFont font, int size) throws IOException {
            ensureSpace();
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(MARGIN, y);
            content.showText(safe(text));
            content.endText();
            y -= LEADING;
        }

        private void ensureSpace() throws IOException {
            if (y < MARGIN) {
                newPage();
            }
        }

        private void newPage() throws IOException {
            if (content != null) {
                content.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        private String[] wrap(String text, int maxChars) {
            String value = fallbackText(text);
            if (value.length() <= maxChars) {
                return new String[] { value };
            }
            return value.replaceAll("(.{1," + maxChars + "})(\\s+|$)", "$1\n").split("\n");
        }

        private String fallbackText(String text) {
            return text == null ? "" : text;
        }

        private String safe(String text) {
            String normalized = Normalizer.normalize(fallbackText(text), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
            return normalized.replaceAll("[^\\x20-\\x7E]", "?");
        }
    }
}
