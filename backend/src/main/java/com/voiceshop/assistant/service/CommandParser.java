package com.voiceshop.assistant.service;

import com.voiceshop.assistant.dto.ParsedCommand;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based "lite NLP" parser.
 *
 * Real NLU services (Dialogflow, Rasa, an LLM function-call, etc.) would
 * normally do this job, but for an 8-hour scope this pattern-matching
 * approach covers the phrasings the brief calls out explicitly ("Add milk",
 * "I need apples", "I want to buy bananas") while staying free, offline and
 * easy to extend - each new phrasing is one more regex or trigger word.
 */
@Component
public class CommandParser {

    private static final Map<String, Integer> WORD_NUMBERS = Map.ofEntries(
            Map.entry("a", 1), Map.entry("an", 1), Map.entry("one", 1), Map.entry("two", 2),
            Map.entry("three", 3), Map.entry("four", 4), Map.entry("five", 5), Map.entry("six", 6),
            Map.entry("seven", 7), Map.entry("eight", 8), Map.entry("nine", 9), Map.entry("ten", 10),
            Map.entry("a couple of", 2), Map.entry("a dozen", 12), Map.entry("dozen", 12)
    );

    private static final String[] ADD_TRIGGERS = {
            "add", "i need", "i want to buy", "i want", "buy", "get me", "get", "put",
            "i'd like", "i would like", "we need"
    };

    private static final String[] REMOVE_TRIGGERS = {
            "remove", "delete", "take off", "i don't need", "cancel"
    };

    private static final String[] SEARCH_TRIGGERS = {
            "find me", "find", "search for", "search", "look for", "show me"
    };

    private static final Pattern PRICE_PATTERN =
            Pattern.compile("under\\s*\\$?\\s*(\\d+(\\.\\d+)?)");

    private static final Pattern QUANTITY_UNIT_PATTERN =
            Pattern.compile("(\\d+|[a-z]+)\\s+(bottles?|kg|kilograms?|grams?|g|liters?|litres?|l|dozen|packs?|boxes?|cans?|bags?)\\s+of\\s+(.+)");

    private static final Pattern MULTI_WORD_QTY_PATTERN =
            Pattern.compile("(a\\s+dozen|a\\s+couple\\s+of)\\s+(.+)");

    private static final Pattern LEADING_QTY_PATTERN =
            Pattern.compile("(\\d+|[a-z]+)\\s+(.+)");

    public ParsedCommand parse(String transcriptRaw) {
        ParsedCommand cmd = new ParsedCommand();
        cmd.setRawTranscript(transcriptRaw);

        if (transcriptRaw == null || transcriptRaw.isBlank()) {
            cmd.setIntent(ParsedCommand.Intent.UNKNOWN);
            return cmd;
        }

        String text = transcriptRaw.trim().toLowerCase()
                .replaceAll("[.!?]+$", "");

        if (text.contains("clear my list") || text.contains("clear the list") || text.equals("clear list")) {
            cmd.setIntent(ParsedCommand.Intent.CLEAR);
            return cmd;
        }

        // Price filter implies a search intent regardless of trigger word used.
        Matcher priceMatcher = PRICE_PATTERN.matcher(text);
        Double maxPrice = null;
        if (priceMatcher.find()) {
            maxPrice = Double.parseDouble(priceMatcher.group(1));
            text = text.substring(0, priceMatcher.start()).trim();
        }

        String remainder;

        if ((remainder = stripTrigger(text, SEARCH_TRIGGERS)) != null || maxPrice != null) {
            cmd.setIntent(ParsedCommand.Intent.SEARCH);
            cmd.setMaxPrice(maxPrice);
            cmd.setItemName(clean(remainder != null ? remainder : text));
            return cmd;
        }

        if ((remainder = stripTrigger(text, REMOVE_TRIGGERS)) != null) {
            remainder = remainder.replaceAll("(from my list|from the list|off my list)$", "").trim();
            cmd.setIntent(ParsedCommand.Intent.REMOVE);
            cmd.setItemName(clean(remainder));
            return cmd;
        }

        if ((remainder = stripTrigger(text, ADD_TRIGGERS)) != null) {
            remainder = remainder.replaceAll("^to my list\\s*", "").replaceAll("to my list$", "").trim();
            cmd.setIntent(ParsedCommand.Intent.ADD);
            applyQuantity(cmd, remainder);
            return cmd;
        }

        // No recognizable trigger word - default to treating the whole
        // utterance as an "add" of whatever was said, which keeps the
        // assistant forgiving of phrasing the trigger list didn't predict.
        cmd.setIntent(ParsedCommand.Intent.ADD);
        applyQuantity(cmd, text);
        return cmd;
    }

    private String stripTrigger(String text, String[] triggers) {
        for (String trigger : triggers) {
            if (text.equals(trigger)) return "";
            if (text.startsWith(trigger + " ")) {
                return text.substring(trigger.length()).trim();
            }
        }
        return null;
    }

    private void applyQuantity(ParsedCommand cmd, String remainder) {
        Matcher multiWordMatcher = MULTI_WORD_QTY_PATTERN.matcher(remainder);
        if (multiWordMatcher.matches()) {
            Integer qty = WORD_NUMBERS.get(multiWordMatcher.group(1).replaceAll("\\s+", " "));
            cmd.setQuantity(qty != null ? qty : 1);
            cmd.setItemName(clean(multiWordMatcher.group(2)));
            return;
        }

        Matcher unitMatcher = QUANTITY_UNIT_PATTERN.matcher(remainder);
        if (unitMatcher.matches()) {
            cmd.setQuantity(parseQuantityToken(unitMatcher.group(1)));
            cmd.setUnit(normalizeUnit(unitMatcher.group(2)));
            cmd.setItemName(clean(unitMatcher.group(3)));
            return;
        }

        Matcher leadingQty = LEADING_QTY_PATTERN.matcher(remainder);
        if (leadingQty.matches()) {
            Integer qty = tryParseQuantityToken(leadingQty.group(1));
            if (qty != null) {
                cmd.setQuantity(qty);
                cmd.setItemName(clean(leadingQty.group(2)));
                return;
            }
        }

        cmd.setItemName(clean(remainder));
    }

    private Integer tryParseQuantityToken(String token) {
        if (token.matches("\\d+")) return Integer.parseInt(token);
        return WORD_NUMBERS.get(token);
    }

    private int parseQuantityToken(String token) {
        Integer parsed = tryParseQuantityToken(token);
        return parsed != null ? parsed : 1;
    }

    private String normalizeUnit(String unit) {
        if (unit.startsWith("bottle")) return "bottle";
        if (unit.startsWith("kilogram") || unit.equals("kg")) return "kg";
        if (unit.startsWith("gram") || unit.equals("g")) return "g";
        if (unit.startsWith("liter") || unit.startsWith("litre") || unit.equals("l")) return "l";
        if (unit.equals("dozen")) return "dozen";
        if (unit.startsWith("pack")) return "pack";
        if (unit.startsWith("box")) return "box";
        if (unit.startsWith("can")) return "can";
        return unit;
    }

    private String clean(String s) {
        if (s == null) return "";
        return s.replaceAll("^(a|an|the|some)\\s+", "").trim();
    }
}
