package net.mofucraft.mofudps.dps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DpsTrackerTest {

    @Test
    void noDamage_isZero() {
        assertEquals(0.0, new DpsTracker(5).getCurrentDps(), 1e-9);
    }

    @Test
    void damageInsideWindow_isSummedAndDividedByWindowSeconds() {
        DpsTracker t = new DpsTracker(5);
        t.addDamage(10);
        t.addDamage(15);
        assertEquals(5.0, t.getCurrentDps(), 1e-9); // (10 + 15) / 5 秒
    }

    @Test
    void windowSecondsChangesTheDivisor() {
        DpsTracker t = new DpsTracker(10);
        t.addDamage(50);
        assertEquals(5.0, t.getCurrentDps(), 1e-9);
    }

    @Test
    void damageOlderThanWindow_isDropped() throws InterruptedException {
        DpsTracker t = new DpsTracker(1);
        t.addDamage(100);
        assertEquals(100.0, t.getCurrentDps(), 1e-9);
        Thread.sleep(1_200);
        assertEquals(0.0, t.getCurrentDps(), 1e-9);
    }

    @Test
    void onlyExpiredEntriesAreDropped() throws InterruptedException {
        DpsTracker t = new DpsTracker(1);
        t.addDamage(100);
        Thread.sleep(1_200);
        t.addDamage(30);
        assertEquals(30.0, t.getCurrentDps(), 1e-9);
    }
}
