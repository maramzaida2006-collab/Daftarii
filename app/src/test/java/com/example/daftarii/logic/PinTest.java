package com.example.daftarii.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PinTest {

    // ---------- PinHasher ----------

    @Test
    public void hasher_samePinSameSalt_matches() {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("1234", salt);
        assertTrue(PinHasher.matches("1234", salt, hash));
    }

    @Test
    public void hasher_wrongPin_doesNotMatch() {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("1234", salt);
        assertFalse(PinHasher.matches("1235", salt, hash));
        assertFalse(PinHasher.matches("", salt, hash));
    }

    @Test
    public void hasher_arabicDigits_sameAsEnglish() {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("1234", salt);
        assertTrue(PinHasher.matches("١٢٣٤", salt, hash));
    }

    @Test
    public void hasher_differentSalts_differentHashes() {
        String a = PinHasher.newSalt();
        String b = PinHasher.newSalt();
        assertNotEquals(a, b);
        assertNotEquals(PinHasher.hash("1234", a), PinHasher.hash("1234", b));
    }

    @Test
    public void hasher_hashIsNotThePin() {
        String salt = PinHasher.newSalt();
        String hash = PinHasher.hash("1234", salt);
        assertFalse(hash.contains("1234"));
        assertEquals(64, hash.length()); // 256 bit = 64 حرف hex
    }

    @Test
    public void hasher_missingData_doesNotMatch() {
        assertFalse(PinHasher.matches("1234", null, "abcd"));
        assertFalse(PinHasher.matches("1234", PinHasher.newSalt(), null));
        assertFalse(PinHasher.matches("1234", PinHasher.newSalt(), "zz"));
    }

    // ---------- PinAttempts ----------

    @Test
    public void attempts_fourFailures_notLocked() {
        PinAttempts.State s = PinAttempts.State.fresh();
        for (int i = 0; i < 4; i++) s = PinAttempts.onFailure(s, 1000);
        assertFalse(PinAttempts.isLocked(s, 1000));
        assertEquals(1, PinAttempts.attemptsLeft(s));
    }

    @Test
    public void attempts_fifthFailure_locks30Seconds() {
        PinAttempts.State s = PinAttempts.State.fresh();
        for (int i = 0; i < 5; i++) s = PinAttempts.onFailure(s, 1000);
        assertTrue(PinAttempts.isLocked(s, 1000));
        assertEquals(30, PinAttempts.secondsLeft(s, 1000));
        assertTrue(PinAttempts.isLocked(s, 30_999));
        assertFalse(PinAttempts.isLocked(s, 31_000));
        assertEquals(0, PinAttempts.secondsLeft(s, 31_000));
        assertEquals(5, PinAttempts.attemptsLeft(s));
    }

    @Test
    public void attempts_secondRound_doublesLockout() {
        PinAttempts.State s = PinAttempts.State.fresh();
        for (int i = 0; i < 10; i++) s = PinAttempts.onFailure(s, 0);
        assertEquals(60, PinAttempts.secondsLeft(s, 0));
    }

    @Test
    public void attempts_lockoutHasMaximum() {
        PinAttempts.State s = PinAttempts.State.fresh();
        for (int i = 0; i < 200; i++) s = PinAttempts.onFailure(s, 0);
        assertEquals(15 * 60, PinAttempts.secondsLeft(s, 0));
    }

    @Test
    public void attempts_successResets() {
        PinAttempts.State s = PinAttempts.State.fresh();
        for (int i = 0; i < 5; i++) s = PinAttempts.onFailure(s, 0);
        s = PinAttempts.onSuccess();
        assertFalse(PinAttempts.isLocked(s, 0));
        assertEquals(5, PinAttempts.attemptsLeft(s));
    }
}
