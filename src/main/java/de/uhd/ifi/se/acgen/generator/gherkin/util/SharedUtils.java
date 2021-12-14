package de.uhd.ifi.se.acgen.generator.gherkin.util;

import java.util.Arrays;
import java.util.List;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreSentence;

/**
 * Provides various utility functions for extraction of acceptance criteria.
 */
public class SharedUtils {

    /**
     * The private constructor of the utility class, preventing instantiation.
     */
    private SharedUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * A list of words which start a conditional sentence or denote a temporal
     * relation between two actions, which can be used to derive actions.
     */
    public static List<String> conditionalStarterStrings = Arrays.asList("if", "when", "once", "whenever", "after", "during");

    /**
     * Identifies the subject of the sentence, which must be the “want” from
     * the “I want” expression of the user story.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param isThirdPerson whether the expected verb is in first (“want”) or
     * third (“wants”) person
     * @return the verb of the sentence
     * @throws TokenNotFoundException if the verb could not be identified by
     * the Stanford CoreNLP tools.
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.util.PreprocessingUtils#identifySubject
     * @see de.uhd.ifi.se.acgen.generator.gherkin.util.ResultUtils#postprocessResultString
     */
    public static IndexedWord identifyVerb(CoreSentence sentence, boolean isThirdPerson) throws TokenNotFoundException {

        // We expect the verb to either be “want”, a non-third person singular
        // present verb (VBP), or “wants”, a third person singular present verb
        // (VBZ)
        String expectedVerb = isThirdPerson ? heSheItDasSMussMit("want") : "want";
        String expectedTag = isThirdPerson ? "VBZ" : "VBP";

        // In many cases, the root object of the dependency graph itself is the
        // verb we are looking for
        IndexedWord root = sentence.dependencyParse().getFirstRoot();
        if (root.word().equalsIgnoreCase(expectedVerb)) {
            return root;
        }

        // If not, it is a child of the root object (as any word in the graph
        // is). First, we look for a word with the appropriate POS tag.
        List<IndexedWord> possibleVerbs = sentence.dependencyParse().getAllNodesByPartOfSpeechPattern(expectedTag);

        // Then, we analyze the contents
        possibleVerbs.removeIf(possibleVerb -> (!possibleVerb.word().equalsIgnoreCase(expectedVerb)));

        if (possibleVerbs.size() == 0) {
            // If there is no matching word, we cannot continue. Throw an
            // appropriate exception.
            throw new TokenNotFoundException("The verb of the user story could not be identified.");
        }

        // There might be multiple children of the root whose POS tag and
        // content match what we looking for. We sort them by their distance
        // to the root object in the graph and return the shallowest word.
        possibleVerbs.sort((possibleVerb, otherPossibleVerb) -> (sentence.dependencyParse().getPathToRoot(possibleVerb).size() - sentence.dependencyParse().getPathToRoot(otherPossibleVerb).size()));
        return possibleVerbs.get(0);
    }

    /**
     * Adds a third-person s to a given verb and handles irregular verbs.
     * 
     * @param verb the verb to be converted to third person
     * @return the verb in third person
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.util.PreprocessingUtils#addSToVerbOfSubject
     * @see SharedUtils#identifyVerb
     * @see de.uhd.ifi.se.acgen.generator.gherkin.util.ResultUtils#postprocessResultString
     * @see <a href="https://www.gymglish.com/en/gymglish/english-grammar/the-s-in-the-third-person-singular-form">https://www.gymglish.com/en/gymglish/english-grammar/the-s-in-the-third-person-singular-form</a>
     * for the rules implemented in this method
     */
    public static String heSheItDasSMussMit(String verb) {
        // Irregular verbs are handled manually
        if (verb.equals("am") || verb.equals("be")) {
            return "is";
        } else if (verb.equals("have")) {
            return "has";
        } else if (verb.equals("do")) {
            return "does";
        } else if (verb.equals("go")) {
            return "goes";
        }

        // Verbs which end with a certain ending require “-es” to be appended
        if (verb.toLowerCase().endsWith("x") || verb.toLowerCase().endsWith("ss") || verb.toLowerCase().endsWith("ch") || verb.toLowerCase().endsWith("sh")) {
            return verb + "es";
        }
        
        // Verbs which end with “y” and a vowel before the “y” require “ies” to
        // be appended to the verb excluding the “y”
        if (verb.toLowerCase().endsWith("y") && !Arrays.asList('a', 'e', 'i', 'o', 'u').contains(verb.charAt(verb.length() - 2))) {
            return verb.substring(0, verb.length() - 1) + "ies";
        }

        // In all other cases, simply append “s”
        return verb + "s";
    }
}
