package de.uhd.ifi.se.acgen.generator.gherkin.util;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreSentence;

/**
 * Provides various utility functions for extraction of expected result
 * acceptance criteria.
 */
public class ResultUtils {
    /**
     * Postprocesses the expected result string to contain only the information
     * not included in other acceptance criteria in a grammatically correct
     * context.
     * 
     * @param resultString a string containing the expected result
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return the postprocessed and sanitized expected result string
     * @throws TokenNotFoundException if the verb of the user story could not
     * be identified by the Stanford CoreNLP tools
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#extractResults
     */
    public static String postprocessResultString(String resultString, CoreSentence sentence) throws TokenNotFoundException {
        String processedResultString = resultString;

        // Identify the verb of the user story and get the following verbs
        IndexedWord verb = SharedUtils.identifyVerb(sentence, true);
        IndexedWord firstWordAfterVerb = sentence.dependencyParse().getNodeByIndex(verb.index() + 1);
        IndexedWord secondWordAfterVerb = sentence.dependencyParse().getNodeByIndex(verb.index() + 2);
        IndexedWord thirdWordAfterVerb = sentence.dependencyParse().getNodeByIndex(verb.index() + 3);

        if (firstWordAfterVerb.tag().equals("TO") && secondWordAfterVerb.tag().equals("VB")) {
            // if the user story is in the form “the user wants to [verb]”
            // (e.g. “the user wants to be able to …”) we need to put the verb
            // after the “to” in third person and remove the “wants” (“[THEN]
            // the user is able to”)
            processedResultString = processedResultString.substring(0, verb.beginPosition()) + SharedUtils.heSheItDasSMussMit(secondWordAfterVerb.word()) + " " + processedResultString.substring(secondWordAfterVerb.endPosition());
        } else if (firstWordAfterVerb.tag().equals("TO") && secondWordAfterVerb.tag().equals("RB") && thirdWordAfterVerb.tag().equals("VB")) {
            // if the user story is in the form “the user wants to [adverb]
            // [verb]” (e.g. “the user wants to efficiently work on …”) we need
            // to put the verb after the “adverb” in third person and remove
            // the “wants” (“[THEN] the user efficiently works on …”)
            processedResultString = processedResultString.substring(0, verb.beginPosition()) + secondWordAfterVerb.word() + " " + SharedUtils.heSheItDasSMussMit(thirdWordAfterVerb.word()) + processedResultString.substring(thirdWordAfterVerb.endPosition());
        } else if (firstWordAfterVerb.tag().equals("IN")) {
            // if the user story is in the form “the user wants [preposition]”
            // (e.g. “the user wants that B happens”) we need to remove
            // everything before the preposition (“[THEN] B happens ”)
            processedResultString = processedResultString.substring(firstWordAfterVerb.endPosition() + 1);
        } else if (firstWordAfterVerb.tag().equals(",") && secondWordAfterVerb.tag().equals("IN")) {
            // if the user story is in the form “the user wants, [preposition]”
            // (e.g. “the user wants, that B happens”) we need to remove
            // everything before the preposition (“[THEN] B happens ”)
            processedResultString = processedResultString.substring(secondWordAfterVerb.endPosition() + 1);
        } else if (getInfinitiveToWord(sentence) != null && getInfinitiveToWord(sentence).endPosition() < processedResultString.length()) {
            IndexedWord infinitiveToWord = getInfinitiveToWord(sentence);
            if (firstWordAfterVerb.tag().equals(",") && SharedUtils.conditionalStarterStrings.contains(secondWordAfterVerb.word().toLowerCase())) {
                // If there is an infinitive-to construction in the sentence,
                // but also a conditional starter string directly at the
                // beginning (e.g. “the user wants, when A happens, to be
                // notified”), we need to remove the comma after “wants”.
                processedResultString = processedResultString.substring(0, verb.beginPosition()) + processedResultString.substring(firstWordAfterVerb.beginPosition());
            } else {
                // If there is an infinitive-to construction in the sentence
                // (e.g. “the user wants the event information to be stored…”),
                // we need to remove everything before and including the
                // “wants”
                processedResultString = processedResultString.substring(firstWordAfterVerb.beginPosition());
            }

            // Also, we need to change the verb from its infinitive form to its
            // third person form, which means adding an “s” if the verb refers
            // to a singular noun.
            int infinitiveToIndex = processedResultString.indexOf("to " + infinitiveToWord.word());
            if (infinitiveToIndex != -1) {
                String infinitiveToString = infinitiveToWord.word();
                if (!childIsPluralNoun(verb, sentence)) {
                    // if there is no plural noun in the subgraph of the verb,
                    // we add an “s”
                    infinitiveToString = SharedUtils.heSheItDasSMussMit(infinitiveToString);
                } else if (infinitiveToString.equalsIgnoreCase("be")) {
                    // if there is a plural noun but the verb is the irregular
                    // “to be”, we change the infinitive form to the correct
                    // form “are”
                    infinitiveToString = "are";
                }

                // We now replace the infinitive form with the third-person
                // form and also remove the “to”, resulting in the exemplary
                // results “[THEN] the user is notified” and “[THEN] the event
                // information is stored”.
                processedResultString = processedResultString.substring(0, infinitiveToIndex) + infinitiveToString + " " + processedResultString.substring(infinitiveToIndex + 3 + infinitiveToWord.word().length());
            }
        } else if (getInfinitiveToWordWithAdverb(sentence) != null && getInfinitiveToWordWithAdverb(sentence).endPosition() < processedResultString.length()) {
            IndexedWord infinitiveToWord = getInfinitiveToWordWithAdverb(sentence);
            IndexedWord adverb = sentence.dependencyParse().getNodeByIndex(infinitiveToWord.index() - 1);

            // If there is an infinitive-to construction with an adverb in the
            // sentence (e.g. “the user wants CoMET to automatically generate
            // …”), we need to remove everything before andnincluding the
            // “wants”
            processedResultString = processedResultString.substring(firstWordAfterVerb.beginPosition());

            int infinitiveToIndex = processedResultString.indexOf("to " + adverb.word() + " " + infinitiveToWord.word());
            if (infinitiveToIndex != -1) {
                String infinitiveToString = infinitiveToWord.word();
                if (!childIsPluralNoun(verb, sentence)) {
                    // if there is no plural noun in the subgraph of the verb,
                    // we add an “s”
                    infinitiveToString = SharedUtils.heSheItDasSMussMit(infinitiveToString);
                } else if (infinitiveToString.equalsIgnoreCase("be")) {
                    // if there is a plural noun but the verb is the irregular
                    // “to be”, we change the infinitive form to the correct
                    // form “are”
                    infinitiveToString = "are";
                }
                if (adverb.word().equalsIgnoreCase("not")) {
                    // If the adverb is not, we change the order of verb and
                    // adverb: “not is” becomes “is not”
                    processedResultString = processedResultString.substring(0, infinitiveToIndex) + infinitiveToString + " " + adverb.word() + " " + processedResultString.substring(infinitiveToIndex + 4 + adverb.word().length() + infinitiveToWord.word().length());
                } else {
                    // Otherwise, we now replace the infinitive form with the
                    // third-person form and also remove the “to”, resulting in
                    // the exemplary result “[THEN] CoMET automatically
                    // generates”.
                    processedResultString = processedResultString.substring(0, infinitiveToIndex) + adverb.word() + " " + infinitiveToString + " " + processedResultString.substring(infinitiveToIndex + 4 + adverb.word().length() + infinitiveToWord.word().length());
                }
            }
        } else {
            // If none of the above cases applies, it is very likely that the
            // user story is of the form “the user wants [object]” (e.g. “the
            // user wants a button which …”). We replace the “wants” with the
            // expression “is provided with” (e.g. “[THEN] the user is provided
            // with a button which …”).
            processedResultString = processedResultString.substring(0, verb.beginPosition()) + " is provided with " + processedResultString.substring(firstWordAfterVerb.beginPosition());
        }

        return sanitizeResultString(processedResultString);
    }

    /**
     * Determines whether a child of a verb in a dependency graph is a plural
     * noun
     * 
     * @param verb the verb to be checked for plural noun children
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return {@code true} if a child of the verb is a plural noun
     * 
     * @see ResultUtils#postprocessResultString
     */
    private static boolean childIsPluralNoun(IndexedWord verb, CoreSentence sentence) {
        for (IndexedWord child : sentence.dependencyParse().getChildList(verb)) {
            if (child.tag().startsWith("NN") && child.tag().endsWith("S") && !child.word().equalsIgnoreCase("details") && child.index() > verb.index()) {
                // The plural noun must also not be the word “details”, since
                // this is often a verb mistaken as a plural noun, and must be
                // located after the verb.
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the first infinitive-to verb in a sentence.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return the first infinitive-to verb, or {@code null} if none is found
     * 
     * @see ResultUtils#postprocessResultString
     */
    private static IndexedWord getInfinitiveToWord(CoreSentence sentence) {
        for (int i = 1; i < sentence.posTags().size(); i++){
            if (sentence.posTags().get(i).equals("VB") && sentence.posTags().get(i - 1).equals("TO")) {
                // If the word at the 0-based index i is a base form verb and
                // the word at the 0-based index i - 1 is an infinitival “to”,
                // get the verb using its 1-based index and return it.
                return sentence.dependencyParse().getNodeByIndex(i + 1);
            }
        }
        return null;
    }

    /**
     * Finds the first infinitive-to verb in a sentence which is preceeded by
     * an adverb.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return the first infinitive-to verb preceeded by an adverb, or
     * {@code null} if none is found
     * 
     * @see ResultUtils#postprocessResultString
     */
    private static IndexedWord getInfinitiveToWordWithAdverb(CoreSentence sentence) {
        for (int i = 2; i < sentence.posTags().size(); i++){
            if (sentence.posTags().get(i).equals("VB") && sentence.posTags().get(i - 1).equals("RB") && sentence.posTags().get(i - 2).equals("TO")) {
                // If the word at the 0-based index i is a base form verb and
                // the word at the 0-based index i - 1 is an adverb and the
                // word at the 0-based index i - 2 is an infinitival “to”, get
                // the verb using its 1-based index and return it.
                return sentence.dependencyParse().getNodeByIndex(i + 1);
            }
        }
        return null;
    }

    /**
     * Removes unnecessary whitespaces and punctuation from a string.
     * 
     * @param resultString the string to be sanitized
     * @return the sanitized string
     * 
     * @see ResultUtils#postprocessResultString
     */
    private static String sanitizeResultString(String resultString) {
        String sanitizedResultString = resultString;
        // Remove leftover commas
        sanitizedResultString = sanitizedResultString.replaceAll(" , ", " ");

        // Replace multiple whitespaces with a single space, thereby removing
        // all the white spaces created when removing information that is part
        // of other acceptance criteria
        sanitizedResultString = sanitizedResultString.replaceAll("\\s+", " ");

        // Remove whitespaces at the beginning
        sanitizedResultString = sanitizedResultString.replaceAll("^\\s+", "");

        // Remove whitespaces, commas and sentence periods at the end
        while (sanitizedResultString.endsWith(",") || sanitizedResultString.endsWith(" ") || sanitizedResultString.endsWith(".")) {
            sanitizedResultString = sanitizedResultString.substring(0, sanitizedResultString.length() - 1);
        }
        return sanitizedResultString;
    }

}
