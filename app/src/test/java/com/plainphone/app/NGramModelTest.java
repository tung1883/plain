package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class NGramModelTest {

    static NGramModel model() {
        return new NGramModel(Arrays.asList(
                "2\t^\t100\ti:40,the:30",
                "2\txin\t50\tchào:30,lỗi:10,phép:5",
                "2\tcảm\t40\tơn:38",
                "2\ti\t100\twant:50,am:30",
                "3\tcảm ơn\t20\tbạn:12,các:4",
                "3\t^ xin\t20\tchào:15"));
    }

    @Test
    public void nextWordsAfterOneWord() {
        List<NGramModel.Pred> p = model().next(null, "xin", 3);
        assertEquals("chào", p.get(0).word);
        assertEquals("lỗi", p.get(1).word);
        assertEquals(3, p.size());
    }

    @Test
    public void twoWordContextWinsOverOne() {
        List<NGramModel.Pred> p = model().next("cảm", "ơn", 3);
        assertEquals("bạn", p.get(0).word);
        assertEquals(0.6, p.get(0).p, 1e-9);
    }

    @Test
    public void sentenceStartIsAContext() {
        assertEquals("i", model().next(null, NGramModel.START, 2).get(0).word);
    }

    @Test
    public void probIsZeroForUnseenPairs() {
        NGramModel m = model();
        assertEquals(0.0, m.prob(null, "xin", "bạn"), 0.0);
        assertTrue(m.prob(null, "xin", "chào") > m.prob(null, "xin", "phép"));
        assertEquals(0.0, m.prob(null, "unknown", "chào"), 0.0);
    }

    @Test
    public void chainFollowsOnlyConfidentWords() {
        NGramModel m = model();
        assertEquals(Arrays.asList("chào"), m.chain(null, NGramModel.START, "xin", 2));
        assertEquals(Arrays.asList("ơn", "bạn"), m.chain(null, "x", "cảm", 2));
        assertEquals(Arrays.asList("want"), m.chain(null, "x", "i", 2));   // exactly 50%: over the bar
    }

    @Test
    public void garbageLinesAreIgnored() {
        NGramModel m = new NGramModel(Arrays.asList("junk", "2\ta\tx\tb:1", "2\ta\t5\tb", "2\ta\t5\tb:2"));
        assertEquals("b", m.next(null, "a", 1).get(0).word);
    }
}
