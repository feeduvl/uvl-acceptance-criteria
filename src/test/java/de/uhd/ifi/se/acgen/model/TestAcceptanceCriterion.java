package de.uhd.ifi.se.acgen.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class TestAcceptanceCriterion {
    
    @Test
    public void testRoleAcceptanceCriterion() {
        AcceptanceCriterion roleAcceptanceCriterion = new AcceptanceCriterion("a developer", AcceptanceCriterionType.ROLE);
        assertEquals(AcceptanceCriterionType.ROLE, roleAcceptanceCriterion.getType());
        assert(roleAcceptanceCriterion.getRawString().equals("a developer"));
        assert(roleAcceptanceCriterion.toString().equals("GIVEN a developer is using the software"));
    }

    @Test
    public void testUIAcceptanceCriterion() {
        AcceptanceCriterion uiAcceptanceCriterion = new AcceptanceCriterion("the example view under \"more examples\"", AcceptanceCriterionType.UI);
        assertEquals(AcceptanceCriterionType.UI, uiAcceptanceCriterion.getType());
        assert(uiAcceptanceCriterion.getRawString().equals("the example view under \"more examples\""));
        assert(uiAcceptanceCriterion.toString().equals("GIVEN the active user interface is the example view under \"more examples\""));
    }

    @Test
    public void testConditionalAcceptanceCriterion() {
        AcceptanceCriterion causeAcceptanceCriterion = new AcceptanceCriterion("he, she, it", AcceptanceCriterionType.CAUSE);
        assertEquals(AcceptanceCriterionType.CAUSE, causeAcceptanceCriterion.getType());
        assert(causeAcceptanceCriterion.getRawString().equals("he, she, it"));
        assert(causeAcceptanceCriterion.toString().equals("WHEN he, she, it"));

        AcceptanceCriterion effectAcceptanceCriterion = new AcceptanceCriterion("das s muss mit", AcceptanceCriterionType.EFFECT);
        assertEquals(AcceptanceCriterionType.EFFECT, effectAcceptanceCriterion.getType());
        assert(effectAcceptanceCriterion.getRawString().equals("das s muss mit"));
        assert(effectAcceptanceCriterion.toString().equals("THEN das s muss mit"));

        AcceptanceCriterion causeInteractionAcceptanceCriterion = new AcceptanceCriterion("on a sample button", AcceptanceCriterionType.CAUSE_INTERACTION);
        assertEquals(AcceptanceCriterionType.CAUSE_INTERACTION, causeInteractionAcceptanceCriterion.getType());
        assert(causeInteractionAcceptanceCriterion.getRawString().equals("on a sample button"));
        assert(causeInteractionAcceptanceCriterion.toString().equals("WHEN the user clicks on a sample button"));
    }

}
