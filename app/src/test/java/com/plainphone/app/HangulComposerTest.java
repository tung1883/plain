package com.plainphone.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class HangulComposerTest {

    @Test
    public void composesSyllables() {
        assertEquals("한글", HangulComposer.compose("gksrmf"));
        assertEquals("안녕", HangulComposer.compose("dkssud"));
        assertEquals("감사", HangulComposer.compose("rkatk"));
    }

    @Test
    public void finalConsonantMovesToTheNextSyllable() {
        assertEquals("가나", HangulComposer.compose("rksk"));   // ㄱㅏㄴㅏ, not 간ㅏ
        assertEquals("앉", HangulComposer.compose("dksw"));
    }

    @Test
    public void vowelAndConsonantCombinations() {
        assertEquals("와", HangulComposer.compose("dhk"));       // ㅗ+ㅏ = ㅘ
        assertEquals("닭", HangulComposer.compose("ekfr"));      // ㄹ+ㄱ final
    }

    @Test
    public void shiftGivesDoubledConsonants() {
        assertEquals("까", HangulComposer.compose("Rk"));
        assertEquals("ㄲ", HangulComposer.jamoFor('r', true));
        assertEquals("ㄱ", HangulComposer.jamoFor('r', false));
        assertEquals("ㅁ", HangulComposer.jamoFor('a', true));   // no shifted jamo: same as lower
        assertNull(HangulComposer.jamoFor('1', false));
    }

    @Test
    public void otherCharactersPassThrough() {
        assertEquals("한1글", HangulComposer.compose("gks1rmf"));
    }

    @Test
    public void upperCaseWithoutAShiftedJamoIsTheLowerOne() {
        assertEquals("한글", HangulComposer.compose("GKSrmF"));
    }
}
