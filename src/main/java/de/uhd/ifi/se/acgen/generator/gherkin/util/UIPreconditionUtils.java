package de.uhd.ifi.se.acgen.generator.gherkin.util;

import java.util.Arrays;
import java.util.List;

import edu.stanford.nlp.pipeline.CoreSentence;

/**
 * Provides various utility functions for extraction of UI precondition
 * acceptance criteria.
 */
public class UIPreconditionUtils {

    /**
     * The private constructor of the utility class, preventing instantiation.
     */
    private UIPreconditionUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Determines the 0-based index of the last word of a UI description.
     * 
     * @param beginIndex the 0-based index of the first word of a UI
     * description
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return the 0-based index of the last word of the UI description
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#extractUIPrecondition
     */
    public static int getEndIndexOfUI(int beginIndex, CoreSentence sentence) {
        List<String> nerTags = sentence.nerTags(); // the named entity recognizer tags
        List<String> posTags = sentence.posTags(); // the POS tags
        List<String> tokensAsStrings = sentence.tokensAsStrings(); // the words of the user story
        int endIndex = 0;

        // for every word in the sentence, beginning with the first word of the
        // UI description
        for (int i = beginIndex; i < nerTags.size(); i++) {
            if (i < endIndex) {
                // if we already found out that the end of the UI description
                // lays beyond the current word
                continue;
            }
            if (!nerTags.get(i).equals("UI") && !posTags.get(i).startsWith("NN") && !posTags.get(i).equals(",") && !posTags.get(i).equals("HYPH")) {
                // If the NER tag of the current word is not “UI” and the
                // current word is not a noun or proper noun, and the current
                // word is not a comma or a hyphen, we may have reached the end
                // of the user story description. However, there are a few 
                // exceptions which are to be checked now.
                endIndex = i - 1;
                List<String> prepositions = Arrays.asList("under", "of", "for");
                if (prepositions.contains(tokensAsStrings.get(i).toLowerCase()) && sentence.dependencyParse().getParent(sentence.dependencyParse().getNodeByIndex(i + 1)).tag().startsWith("NN")) {
                    // If the current word is one of the above prepositions and
                    // its parent in the dependency graph (i + 1 because of the
                    // 1-based index in the graph) is a noun or proper noun,
                    // we have not reached the end of the user story
                    // description. The noun or proper noun (e.g. “EVENT” in
                    // the expression “the event details of an EVENT”) is part
                    // of the user story, so its 0-based index is the smallest
                    // possible value for the end index.
                    if (tokensAsStrings.get(i).equals("for") && posTags.get(i + 1).equals("VBG")) {
                        // However, if the preposition is “for” and the
                        // following verb is a gerund or a present participle
                        // verb as in “I want to have an icon in the
                        // registrations view FOR importing registrations”, we
                        // have not not reached (i.e., we have indeed reached)
                        // the end of the UI description.
                        break;
                    }
                    endIndex = sentence.dependencyParse().getParent(sentence.dependencyParse().getNodeByIndex(i + 1)).index() - 1;
                } else if (tokensAsStrings.get(i).equals("in") && (nerTags.get(i + 1).equals("UI") || tokensAsStrings.get(i + 2).equalsIgnoreCase("list") || tokensAsStrings.get(i + 1).equalsIgnoreCase("CoMET"))) {
                    // If the current word is “in” followed by another UI
                    // description, as in “in the event cockpit IN the event
                    // details” or followed by a specific list, as in “in the
                    // event cockpit IN the LIST of participants” or followed
                    // by “CoMET”, as in “in the event cockpit in CoMET”, we
                    // have not reached the end of the UI description since the
                    // aforementioned words following “in” are part of the UI
                    // description.
                    endIndex = i + 2;
                } else if (posTags.get(i).equals("``") && posTags.subList(i + 1, posTags.size()).contains("''")) {
                    // If the current token are opening quotation marks and the
                    // user story sentence contains closing quotation marks
                    // after it, we have not reached the end of the UI
                    // description since the content of the quotation marks is
                    // part of the UI description, as in “the view "List
                    // Registrations"”.
                    endIndex = i + 2 + posTags.subList(i + 1, posTags.size()).indexOf("''");
                } else if (posTags.get(i).equals("-LRB-") && posTags.get(i + 2).equals("-RRB-") && tokensAsStrings.get(i + 1).equals("ET")) {
                    // If the current token is an opening parenthesis, the next
                    // token is “ET” and the following token is a closing
                    // parenthesis, we have not reached the end of the UI
                    // description since “(ET)” describes the UI selection
                    // “event type Developer Days (ET)” and is part of the UI
                    // description.
                    endIndex = i + 3;
                } else if (tokensAsStrings.get(i).equals("\"") && tokensAsStrings.subList(i + 1, tokensAsStrings.size()).contains("\"")) {
                    // If the current token are quotation marks and the user
                    // story sentence contains another quotation mark token
                    // after it, we have not reached the end of the UI
                    // description since the content of the quotation marks is
                    // part of the UI description, as in “the view "List
                    // Registrations"”.
                    endIndex = i + 2 + tokensAsStrings.subList(i + 1, tokensAsStrings.size()).indexOf("\"");
                } else if (tokensAsStrings.get(i).equals(">") && tokensAsStrings.get(i - 1).equals("-")) {
                    // If the current token is “>” and the previous token is a
                    // hyphen, we have not reached the end of the UI
                    // description, since the arrow “->” points to another UI
                    // description, as in “in the DSBC under Contacts -> List
                    // Contacts”. 
                    endIndex = i + 1;
                } else {
                    // If none of the exceptions occured, we are indeed at the
                    // end of the UI description.
                    break;
                }
            }
        }
        return endIndex;
    }
    
}
