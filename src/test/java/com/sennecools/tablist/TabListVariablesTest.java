package com.sennecools.tablist;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TabListVariablesTest {

    private static final String S = "§";
    private static final String RED = hex("ff0000");
    private static final String GREEN = hex("00ff00");
    private static final String BLUE = hex("0000ff");

    /** Builds the legacy hex form: section sign + x, then every digit prefixed with a section sign. */
    private static String hex(String digits) {
        StringBuilder sb = new StringBuilder(S).append('x');
        for (char c : digits.toCharArray()) {
            sb.append(S).append(c);
        }
        return sb.toString();
    }

    // ---- convertColorCodes: legacy and hex codes ----

    @Test
    void convertsAmpersandColorAndFormatCodes() {
        assertEquals(S + "aHi", TabListVariables.convertColorCodes("&aHi"));
        assertEquals(S + "c" + S + "lBold" + S + "r plain", TabListVariables.convertColorCodes("&c&lBold&r plain"));
        assertEquals(S + "AHi", TabListVariables.convertColorCodes("&AHi"));
    }

    @Test
    void leavesUnknownCodesAndLoneAmpersandsAlone() {
        assertEquals("&z", TabListVariables.convertColorCodes("&z"));
        assertEquals("Tom & Jerry", TabListVariables.convertColorCodes("Tom & Jerry"));
        assertEquals("trailing &", TabListVariables.convertColorCodes("trailing &"));
        assertEquals("", TabListVariables.convertColorCodes(""));
    }

    @Test
    void expandsHexColorKeepingInputCase() {
        assertEquals(hex("FF5555") + "x", TabListVariables.convertColorCodes("&#FF5555x"));
        assertEquals(hex("ff5555") + "x", TabListVariables.convertColorCodes("&#ff5555x"));
    }

    @Test
    void doesNotTreatMalformedHexAsColor() {
        assertEquals("&#GG0000", TabListVariables.convertColorCodes("&#GG0000"));
        // Only five digits: no hex colour, and "&#" is not a legacy code either.
        assertEquals("&#FF555", TabListVariables.convertColorCodes("&#FF555"));
    }

    // ---- convertColorCodes: gradients ----

    @Test
    void gradientStartsOnFirstStopAndEndsOnLastStop() {
        assertEquals(RED + "a" + BLUE + "b",
                TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>ab</gradient>"));
    }

    @Test
    void gradientInterpolatesLinearlyBetweenTwoStops() {
        // Middle of #000000 -> #646464 (100 per channel) is 50 = 0x32.
        assertEquals(hex("000000") + "a" + hex("323232") + "b" + hex("646464") + "c",
                TabListVariables.convertColorCodes("<gradient:#000000:#646464>abc</gradient>"));
    }

    @Test
    void threeStopGradientHitsTheMiddleStopOnTheMiddleCharacter() {
        assertEquals(RED + "a" + GREEN + "b" + BLUE + "c",
                TabListVariables.convertColorCodes("<gradient:#FF0000:#00FF00:#0000FF>abc</gradient>"));
    }

    @Test
    void threeStopGradientInterpolatesWithinEachSegment() {
        // Five characters over two segments: positions 1 and 3 are the segment midpoints.
        String result = TabListVariables.convertColorCodes("<gradient:#000000:#646464:#000000>abcde</gradient>");
        assertEquals(hex("000000") + "a" + hex("323232") + "b" + hex("646464") + "c"
                + hex("323232") + "d" + hex("000000") + "e", result);
    }

    @Test
    void singleCharacterGradientUsesFirstStop() {
        assertEquals(RED + "a", TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>a</gradient>"));
    }

    @Test
    void emptyGradientBodyProducesNothing() {
        assertEquals("", TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF></gradient>"));
    }

    @Test
    void spacesInsideGradientCountAsCharacters() {
        // "a b" is three characters, so the space takes the midpoint colour.
        assertEquals(hex("000000") + "a" + hex("323232") + " " + hex("646464") + "b",
                TabListVariables.convertColorCodes("<gradient:#000000:#646464>a b</gradient>"));
    }

    @Test
    void formatModifierInsideGradientIsRepeatedAfterEveryColor() {
        // A colour code resets modifiers on the client, so bold has to follow each hex code.
        assertEquals(RED + S + "la" + BLUE + S + "lb",
                TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>&lab</gradient>"));
    }

    @Test
    void formatModifierInsideGradientOnlyAppliesFromWhereItAppears() {
        assertEquals(RED + "a" + BLUE + S + "lb",
                TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>a&lb</gradient>"));
    }

    @Test
    void colorCodeInsideGradientIsDroppedAndClearsModifiers() {
        // The gradient owns the colour: "&c" is not emitted and is not counted as a character,
        // but like any colour code it ends the bold that was active before it.
        assertEquals(RED + S + "la" + BLUE + "b",
                TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>&la&cb</gradient>"));
    }

    @Test
    void tabStyleGradientMatchesMiniMessageStyle() {
        String mini = TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>abc</gradient>");
        String tab = TabListVariables.convertColorCodes("<#FF0000>abc</#0000FF>");
        assertEquals(mini, tab);
        assertEquals(RED + "a" + hex("800080") + "b" + BLUE + "c", tab);
    }

    @Test
    void textAroundGradientIsPreservedAndStillConverted() {
        assertEquals(S + "7x" + RED + "a" + BLUE + "b" + S + "ry",
                TabListVariables.convertColorCodes("&7x<gradient:#FF0000:#0000FF>ab</gradient>&ry"));
    }

    @Test
    void twoGradientsOnOneLineAreIndependent() {
        assertEquals(RED + "a" + BLUE + "b-" + GREEN + "c" + RED + "d",
                TabListVariables.convertColorCodes(
                        "<gradient:#FF0000:#0000FF>ab</gradient>-<gradient:#00FF00:#FF0000>cd</gradient>"));
    }

    @Test
    void gradientWithSingleStopOrMissingCloseTagIsLeftAsText() {
        assertEquals("<gradient:#FF0000>ab</gradient>",
                TabListVariables.convertColorCodes("<gradient:#FF0000>ab</gradient>"));
        assertEquals("<gradient:#FF0000:#0000FF>ab",
                TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>ab"));
    }

    @Test
    @Disabled("Known bug: applyGradient only strips the already-converted '&x&R&R&G&G&B&B' form, which never "
            + "occurs at that stage, so a '&#RRGGBB' code inside a gradient is rendered as literal text.")
    void hexColorInsideGradientIsNotShownAsText() {
        String result = TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>&#00FF00ab</gradient>");
        assertEquals(RED + "a" + BLUE + "b", result);
    }

    // ---- parseColoredText ----

    @Test
    void nullAndEmptyTextGiveAnEmptyComponent() {
        assertEquals("", TabListVariables.parseColoredText(null).getString());
        assertTrue(TabListVariables.parseColoredText(null).getSiblings().isEmpty());
        assertEquals("", TabListVariables.parseColoredText("").getString());
        assertTrue(TabListVariables.parseColoredText("").getSiblings().isEmpty());
    }

    @Test
    void plainTextBecomesOneUnstyledSibling() {
        List<Component> parts = TabListVariables.parseColoredText("Hello").getSiblings();
        assertEquals(1, parts.size());
        assertEquals("Hello", parts.get(0).getString());
        assertTrue(parts.get(0).getStyle().isEmpty());
    }

    @Test
    void legacyColorCodeStylesTheFollowingTextAndIsRemovedFromIt() {
        Component component = TabListVariables.parseColoredText(S + "cHi");
        List<Component> parts = component.getSiblings();
        assertEquals("Hi", component.getString());
        assertEquals(1, parts.size());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), parts.get(0).getStyle().getColor());
    }

    @Test
    void legacyCodesAreCaseInsensitive() {
        Component component = TabListVariables.parseColoredText(S + "CHi" + S + "LBold");
        List<Component> parts = component.getSiblings();
        assertEquals("HiBold", component.getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), parts.get(0).getStyle().getColor());
        assertTrue(parts.get(1).getStyle().isBold());
    }

    @Test
    void hexSequenceBecomesRgbColor() {
        Component component = TabListVariables.parseColoredText(hex("FF5555") + "A");
        List<Component> parts = component.getSiblings();
        assertEquals("A", component.getString());
        assertEquals(1, parts.size());
        assertEquals(TextColor.fromRgb(0xFF5555), parts.get(0).getStyle().getColor());
    }

    @Test
    void textIsSplitIntoOneSiblingPerStyleRun() {
        Component component = TabListVariables.parseColoredText("a" + S + "cb" + S + "9c");
        List<Component> parts = component.getSiblings();
        assertEquals("abc", component.getString());
        assertEquals(3, parts.size());
        assertNull(parts.get(0).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), parts.get(1).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.BLUE), parts.get(2).getStyle().getColor());
    }

    @Test
    void modifiersStack() {
        Style style = TabListVariables.parseColoredText(S + "l" + S + "o" + S + "n" + S + "m" + S + "kA")
                .getSiblings().get(0).getStyle();
        assertTrue(style.isBold());
        assertTrue(style.isItalic());
        assertTrue(style.isUnderlined());
        assertTrue(style.isStrikethrough());
        assertTrue(style.isObfuscated());
        assertNull(style.getColor());
    }

    @Test
    void namedColorResetsModifiersLikeVanillaLegacyCodes() {
        List<Component> parts = TabListVariables.parseColoredText(S + "lA" + S + "cB").getSiblings();
        assertEquals(2, parts.size());
        assertTrue(parts.get(0).getStyle().isBold());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), parts.get(1).getStyle().getColor());
        assertFalse(parts.get(1).getStyle().isBold());
    }

    @Test
    void hexColorKeepsActiveModifiers() {
        // Current behaviour differs from named colours: a hex colour does not clear bold.
        // Gradient output relies on the modifier following the hex code, which works either way.
        List<Component> parts = TabListVariables.parseColoredText(S + "lA" + hex("0000ff") + "B").getSiblings();
        assertEquals(2, parts.size());
        assertEquals(TextColor.fromRgb(0x0000FF), parts.get(1).getStyle().getColor());
        assertTrue(parts.get(1).getStyle().isBold());
    }

    @Test
    void resetClearsColorAndModifiers() {
        List<Component> parts = TabListVariables.parseColoredText(S + "c" + S + "lA" + S + "rB").getSiblings();
        assertEquals(2, parts.size());
        assertTrue(parts.get(0).getStyle().isBold());
        assertEquals("B", parts.get(1).getString());
        assertTrue(parts.get(1).getStyle().isEmpty());
    }

    @Test
    void codesWithoutTextProduceNoSiblings() {
        Component component = TabListVariables.parseColoredText(S + "c" + S + "l");
        assertEquals("", component.getString());
        assertTrue(component.getSiblings().isEmpty());
    }

    @Test
    void unknownCodeAndTrailingSectionSignAreKeptAsText() {
        assertEquals(S + "zHi", TabListVariables.parseColoredText(S + "zHi").getString());
        assertEquals("Hi" + S, TabListVariables.parseColoredText("Hi" + S).getString());
    }

    @Test
    void truncatedOrInvalidHexSequenceDoesNotThrowAndKeepsFollowingText() {
        // Too short to be a hex colour: "§x" is not a code, so it stays as text like any unknown code,
        // and the "§F" after it is still read as an ordinary (white) colour code.
        Component truncated = assertDoesNotThrow(() -> TabListVariables.parseColoredText(S + "x" + S + "FHi"));
        assertEquals(S + "xHi", truncated.getString());
        List<Component> truncatedParts = truncated.getSiblings();
        assertEquals(2, truncatedParts.size());
        assertNull(truncatedParts.get(0).getStyle().getColor());
        assertEquals("Hi", truncatedParts.get(1).getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.WHITE), truncatedParts.get(1).getStyle().getColor());

        // 14 characters long but with a non-hex digit: not a colour, the text after it must survive.
        Component invalid = assertDoesNotThrow(() -> TabListVariables.parseColoredText(hex("FF55ZZ") + "Hi"));
        assertTrue(invalid.getString().endsWith("Hi"));
        assertTrue(invalid.getSiblings().stream()
                .noneMatch(part -> TextColor.fromRgb(0xFF5555).equals(part.getStyle().getColor())));
    }

    // ---- both stages together ----

    @Test
    void gradientRoundTripsIntoPerCharacterRgbComponents() {
        Component component = TabListVariables.parseColoredText(
                TabListVariables.convertColorCodes("<gradient:#FF0000:#0000FF>&lab</gradient>"));
        List<Component> parts = component.getSiblings();
        assertEquals("ab", component.getString());
        assertEquals(2, parts.size());
        assertEquals(TextColor.fromRgb(0xFF0000), parts.get(0).getStyle().getColor());
        assertEquals(TextColor.fromRgb(0x0000FF), parts.get(1).getStyle().getColor());
        assertTrue(parts.get(0).getStyle().isBold());
        assertTrue(parts.get(1).getStyle().isBold());
    }

    @Test
    void ampersandHexRoundTripsIntoRgbComponent() {
        Component component = TabListVariables.parseColoredText(
                TabListVariables.convertColorCodes("&#12aBcD&oText&r!"));
        List<Component> parts = component.getSiblings();
        assertEquals("Text!", component.getString());
        assertEquals(TextColor.fromRgb(0x12ABCD), parts.get(0).getStyle().getColor());
        assertTrue(parts.get(0).getStyle().isItalic());
        assertTrue(parts.get(1).getStyle().isEmpty());
    }
}
