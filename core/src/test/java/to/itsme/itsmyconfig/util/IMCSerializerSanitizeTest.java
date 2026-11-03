package to.itsme.itsmyconfig.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class IMCSerializerSanitizeTest {

    @Test
    void stripsSingleTrailingColorTag() {
        assertEquals("<#ff0000>hello", IMCSerializer.sanitize("<#ff0000>hello</#ff0000>"));
    }

    @Test
    void stripsMultipleTrailingColorTags() {
        assertEquals("hello", IMCSerializer.sanitize("hello</#ff0000></#00FF00></#123abc>"));
    }

    @Test
    void stripsEntireStringWhenItIsOnlyTags() {
        assertEquals("", IMCSerializer.sanitize("</#123456></#ABCDEF>"));
    }

    @Test
    void keepsNonTrailingColorTag() {
        assertEquals("a</#112233>b", IMCSerializer.sanitize("a</#112233>b</#445566>"));
    }

    @Test
    void keepsInvalidHexTag() {
        assertEquals("hello</#GGGGGG>", IMCSerializer.sanitize("hello</#GGGGGG>"));
    }

    @Test
    void keepsTagWithTooFewHexDigits() {
        assertEquals("hello</#12345>", IMCSerializer.sanitize("hello</#12345>"));
    }

    @Test
    void keepsTagWithTooManyHexDigits() {
        assertEquals("hello</#1234567>", IMCSerializer.sanitize("hello</#1234567>"));
    }

    @Test
    void keepsTagThatIsNotAtTheEnd() {
        assertEquals("hello</#123456> tail", IMCSerializer.sanitize("hello</#123456> tail"));
    }

    @Test
    void leavesPlainTextUntouched() {
        final String plain = "plain chat message";
        assertSame(plain, IMCSerializer.sanitize(plain));
    }

    @Test
    void leavesShortInputUntouched() {
        final String shortInput = "</#12345";
        assertSame(shortInput, IMCSerializer.sanitize(shortInput));
    }

    @Test
    void handlesNull() {
        assertNull(IMCSerializer.sanitize(null));
    }

}
