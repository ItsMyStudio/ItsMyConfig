package to.itsme.itsmyconfig.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionsTest {

    @Test
    void resolveVersionExact_classicBukkitSnapshot() {
        assertEquals("1.21.4", Versions.resolveVersionExact("1.21.4-R0.1-SNAPSHOT"));
        assertEquals("1.20.4", Versions.resolveVersionExact("1.20.4-R0.1-SNAPSHOT"));
        assertEquals("1.16.5", Versions.resolveVersionExact("1.16.5-R0.1-SNAPSHOT"));
    }

    @Test
    void resolveVersionExact_paper26BuildClassifier() {
        // Leaf/Paper 26.2: "26.2.build.24-alpha" → NumberFormatException before the fix
        assertEquals("26.2", Versions.resolveVersionExact("26.2.build.24-alpha"));
        assertEquals("26.2", Versions.resolveVersionExact("26.2.build.24"));
        assertEquals("26.1.2", Versions.resolveVersionExact("26.1.2"));
        assertEquals("26.2", Versions.resolveVersionExact("26.2"));
    }

    @Test
    void resolveVersionExact_edgeCases() {
        assertEquals("0.0.0", Versions.resolveVersionExact(null));
        assertEquals("0.0.0", Versions.resolveVersionExact(""));
        assertEquals("0.0.0", Versions.resolveVersionExact("build.only"));
    }

    @Test
    void parseVersionParts_mapsMajorMinorPatch() {
        assertArrayEquals(new int[]{1, 21, 4}, Versions.parseVersionParts("1.21.4"));
        assertArrayEquals(new int[]{26, 2, 0}, Versions.parseVersionParts("26.2"));
        assertArrayEquals(new int[]{26, 1, 2}, Versions.parseVersionParts("26.1.2"));
        assertArrayEquals(new int[]{1, 16, 0}, Versions.parseVersionParts("1.16"));
    }

    @Test
    void paper26IsNewerThanLegacyMinecraftChecks() {
        final int[] parts = Versions.parseVersionParts(Versions.resolveVersionExact("26.2.build.24-alpha"));
        final int major = parts[0];
        final int minor = parts[1];
        final int patch = parts[2];

        // Same logic as Versions.isBelow / isOrOver, using parsed values
        assertEquals(26, major);
        assertEquals(2, minor);
        assertEquals(0, patch);

        // isBelow(1, 16, 5) must be false on 26.2 (plugin should enable)
        assertEquals(false, compareBelow(major, minor, patch, 1, 16, 5));
        // isOrOver(1, 21, 5) must be true (modern click/hover JSON)
        assertEquals(true, compareOrOver(major, minor, patch, 1, 21, 5));
        // isOver(1, 19, 0) must be true (action bar path)
        assertEquals(true, compareOver(major, minor, patch, 1, 19, 0));
        // isBelow(1, 16, 0) must be false (ProtocolLib adventure path)
        assertEquals(false, compareBelow(major, minor, patch, 1, 16, 0));
    }

    private static boolean compareBelow(int major, int minor, int patch, int tMajor, int tMinor, int tPatch) {
        if (major < tMajor) return true;
        if (major == tMajor) {
            if (minor < tMinor) return true;
            if (minor == tMinor) return patch < tPatch;
        }
        return false;
    }

    private static boolean compareOver(int major, int minor, int patch, int tMajor, int tMinor, int tPatch) {
        if (major > tMajor) return true;
        if (major == tMajor) {
            if (minor > tMinor) return true;
            if (minor == tMinor) return patch > tPatch;
        }
        return false;
    }

    private static boolean compareOrOver(int major, int minor, int patch, int tMajor, int tMinor, int tPatch) {
        return (major == tMajor && minor == tMinor && patch == tPatch)
                || compareOver(major, minor, patch, tMajor, tMinor, tPatch);
    }
}
