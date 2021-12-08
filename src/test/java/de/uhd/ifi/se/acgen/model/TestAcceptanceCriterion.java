package de.uhd.ifi.se.acgen.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestAcceptanceCriterion {
    
    @Test
    public void testRoleAcceptanceCriterion() {
        AcceptanceCriterion roleAcceptanceCriterion = new AcceptanceCriterion("a developer", AcceptanceCriterionType.ROLE);
        assertEquals(AcceptanceCriterionType.ROLE, roleAcceptanceCriterion.getType());
        assertFalse(roleAcceptanceCriterion.getType().isLog());
        assert(roleAcceptanceCriterion.getRawString().equals("a developer"));
        assert(roleAcceptanceCriterion.toString().equals("GIVEN a developer is using the software"));
    }

    @Test
    public void testUIAcceptanceCriterion() {
        AcceptanceCriterion uiAcceptanceCriterion = new AcceptanceCriterion("the example view under \"more examples\"", AcceptanceCriterionType.UI);
        assertEquals(AcceptanceCriterionType.UI, uiAcceptanceCriterion.getType());
        assertFalse(uiAcceptanceCriterion.getType().isLog());
        assert(uiAcceptanceCriterion.getRawString().equals("the example view under \"more examples\""));
        assert(uiAcceptanceCriterion.toString().equals("GIVEN the active user interface is the example view under \"more examples\""));
    }

    @Test
    public void testConditionalAcceptanceCriterion() {
        AcceptanceCriterion causeAcceptanceCriterion = new AcceptanceCriterion("he, she, it", AcceptanceCriterionType.CAUSE);
        assertEquals(AcceptanceCriterionType.CAUSE, causeAcceptanceCriterion.getType());
        assertFalse(causeAcceptanceCriterion.getType().isLog());
        assert(causeAcceptanceCriterion.getRawString().equals("he, she, it"));
        assert(causeAcceptanceCriterion.toString().equals("WHEN he, she, it"));

        AcceptanceCriterion effectAcceptanceCriterion = new AcceptanceCriterion("das s muss mit", AcceptanceCriterionType.EFFECT);
        assertEquals(AcceptanceCriterionType.EFFECT, effectAcceptanceCriterion.getType());
        assertFalse(effectAcceptanceCriterion.getType().isLog());
        assert(effectAcceptanceCriterion.getRawString().equals("das s muss mit"));
        assert(effectAcceptanceCriterion.toString().equals("THEN das s muss mit"));

        AcceptanceCriterion causeAcceptanceCriterionInReason = new AcceptanceCriterion("he, she, it", AcceptanceCriterionType.CAUSE_IN_REASON);
        assertEquals(AcceptanceCriterionType.CAUSE_IN_REASON, causeAcceptanceCriterionInReason.getType());
        assertFalse(causeAcceptanceCriterionInReason.getType().isLog());
        assert(causeAcceptanceCriterionInReason.getRawString().equals("he, she, it"));
        assert(causeAcceptanceCriterionInReason.toString().equals("WHEN he, she, it"));

        AcceptanceCriterion effectAcceptanceCriterionInReason = new AcceptanceCriterion("das s muss mit", AcceptanceCriterionType.EFFECT_IN_REASON);
        assertEquals(AcceptanceCriterionType.EFFECT_IN_REASON, effectAcceptanceCriterionInReason.getType());
        assertFalse(effectAcceptanceCriterionInReason.getType().isLog());
        assert(effectAcceptanceCriterionInReason.getRawString().equals("das s muss mit"));
        assert(effectAcceptanceCriterionInReason.toString().equals("THEN das s muss mit"));

        assertTrue(causeAcceptanceCriterion.compareTo(effectAcceptanceCriterion) < 0);
        assertTrue(causeAcceptanceCriterion.compareTo(causeAcceptanceCriterionInReason) < 0);
        assertTrue(causeAcceptanceCriterionInReason.compareTo(effectAcceptanceCriterionInReason) < 0);
    }

    @Test
    public void testLogAcceptanceCriterion() {
        AcceptanceCriterion errorAcceptanceCriterion = new AcceptanceCriterion("This is a severe error!", AcceptanceCriterionType.ERROR);
        assertEquals(AcceptanceCriterionType.ERROR, errorAcceptanceCriterion.getType());
        assertTrue(errorAcceptanceCriterion.getType().isLog());
        assert(errorAcceptanceCriterion.getRawString().equals("This is a severe error!"));
        assert(errorAcceptanceCriterion.toString().equals("ERROR: This is a severe error!"));

        AcceptanceCriterion warningAcceptanceCriterion = new AcceptanceCriterion("This is a less severe warning.", AcceptanceCriterionType.WARNING);
        assertEquals(AcceptanceCriterionType.WARNING, warningAcceptanceCriterion.getType());
        assertTrue(warningAcceptanceCriterion.getType().isLog());
        assert(warningAcceptanceCriterion.getRawString().equals("This is a less severe warning."));
        assert(warningAcceptanceCriterion.toString().equals("WARNING: This is a less severe warning."));

        AcceptanceCriterion infoAcceptanceCriterion = new AcceptanceCriterion("This is helpful information.", AcceptanceCriterionType.INFO);
        assertEquals(AcceptanceCriterionType.INFO, infoAcceptanceCriterion.getType());
        assertTrue(infoAcceptanceCriterion.getType().isLog());
        assert(infoAcceptanceCriterion.getRawString().equals("This is helpful information."));
        assert(infoAcceptanceCriterion.toString().equals("INFO: This is helpful information."));

        AcceptanceCriterion debugAcceptanceCriterion = new AcceptanceCriterion("Beep boop!", AcceptanceCriterionType.DEBUG);
        assertEquals(AcceptanceCriterionType.DEBUG, debugAcceptanceCriterion.getType());
        assertTrue(debugAcceptanceCriterion.getType().isLog());
        assert(debugAcceptanceCriterion.getRawString().equals("Beep boop!"));
        assert(debugAcceptanceCriterion.toString().equals("DEBUG: Beep boop!"));

        assertTrue(errorAcceptanceCriterion.compareTo(warningAcceptanceCriterion) < 0);
        assertTrue(warningAcceptanceCriterion.compareTo(infoAcceptanceCriterion) < 0);
        assertTrue(infoAcceptanceCriterion.compareTo(debugAcceptanceCriterion) < 0);
    }
}
