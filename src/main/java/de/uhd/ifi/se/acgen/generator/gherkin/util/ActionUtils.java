package de.uhd.ifi.se.acgen.generator.gherkin.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.uhd.ifi.se.acgen.model.AcceptanceCriterion;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterionType;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreSentence;

/**
 * Provides various utility functions for extraction of action acceptance
 * criteria.
 */
public class ActionUtils {

    /**
     * The private constructor of the utility class, preventing instantiation.
     */
    private ActionUtils() {
        throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Checks whether the expression “as soon as” is present at the specified
     * position in the given sentence. The 1-based index must point at the
     * second “as”.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param i the 1-based index to check
     * @return {@code true} if “as soon as” is present at the specified
     * position
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#extractActions
     * @see ActionUtils#extractActionFromConditionalStarterWord
     */
    public static boolean isAsSoonAs(CoreSentence sentence, int i) {
        return sentence.dependencyParse().getNodeByIndex(i).word().equalsIgnoreCase("as") && sentence.dependencyParse().getNodeByIndex(i - 1).word().equalsIgnoreCase("soon") && sentence.dependencyParse().getNodeByIndex(i - 2).word().equalsIgnoreCase("as");
    }

    /**
     * Extracts an action that begins at a conditional starter word.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param userStoryString a string containing the user story
     * @param conditionalStarterWord the conditional starter word with which
     * the action begins
     * @param indexSoThat the 1-based index of the “so that” keyword of the
     * user story or the 1-based index of the last word, if there is no reason
     * in the user story.
     * @return a list containing zero or one acceptance criteria with an action
     * from the user story and optionally an acceptance criterion with an
     * expected result, if the action is part of the reason of the user story
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#extractActions
     */
    public static List<AcceptanceCriterion> extractActionFromConditionalStarterWord(CoreSentence sentence, String userStoryString, IndexedWord conditionalStarterWord, int indexSoThat) {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();

        // Get the root of the conditional sentence, whose subgraph in many
        // cases contains exactly the words of the conditional sentence, and
        // extract those words.
        IndexedWord root = getRootOfAction(sentence, conditionalStarterWord);
        List<IndexedWord> actionWords = new ArrayList<IndexedWord>();
        actionWords.addAll(sentence.dependencyParse().getSubgraphVertices(root));

        if (actionWords.size() < 2) {
            // If we are unable to find more words being part of the
            // conditional sentence, we cannot continue and return the empty
            // list.
            return acceptanceCriteria;
        }

        // We need to sort the list of action words since “getSubgraphVertices”
        // returns a set, not a list.
        actionWords.sort((word, otherWord) -> word.index() - otherWord.index());

        // Words that come before the conditional starter word cannot be part
        // of the conditional sentence.
        actionWords.removeIf(word -> word.index() < conditionalStarterWord.index());

        // Since the words to be extracted will be prepended with WHEN, we do
        // not need to extract the conditional starter word itself.
        int beginIndex = conditionalStarterWord.index() + 1;

        // However, if the conditional sentence is prepended by a duration
        // (which is recognized by the Named Entity Recognizer) such as “I want
        // B to happen three months after A”, the duration shall be part of the
        // extracted words.
        if (sentence.nerTags().get(conditionalStarterWord.index() - 1 - 1).equals("DURATION")) {
            beginIndex = getBeginIndexOfDuration(conditionalStarterWord, sentence);
        }

        // We need to determine the index of the last word that still is part
        // of the conditional sentence.
        int endIndex = determineActionEndIndex(actionWords, conditionalStarterWord, indexSoThat, sentence);

        int wordsInSentenceCount = sentence.dependencyParse().getSubgraphVertices(sentence.dependencyParse().getFirstRoot()).size();
        if (conditionalStarterWord.index() > indexSoThat && endIndex >= wordsInSentenceCount - 1) {
            // If the conditional sentence is part of the reason of the user
            // story and includes all words up to the last word in the user
            // story, we do not have words which may create an expected result
            // to this action, so we cannot continue and return the empty list.
            return acceptanceCriteria;
        }

        // We determine the 1-based indices of the first and last action words
        // and then retrieve the begin and end position of these words in the
        // user story string. 
        int beginPosition = sentence.dependencyParse().getNodeByIndex(beginIndex).beginPosition();
        int endPosition = sentence.dependencyParse().getNodeByIndex(endIndex).endPosition();
        String actionString = userStoryString.substring(beginPosition, endPosition);

        if (sentence.nerTags().get(conditionalStarterWord.index() - 1 - 1).equals("DURATION")) {
            // If the conditional sentence is prepended by a duration as
            // described above, the exemplary sentence “I want B to happen
            // three months after A” is rearranged to “[WHEN] three months
            // passed after A”
            actionString = userStoryString.substring(beginPosition, conditionalStarterWord.beginPosition()) + "passed " + userStoryString.substring(conditionalStarterWord.beginPosition(), endPosition);
        } else if (isAsSoonAs(sentence, beginIndex - 1)) {
            // If the conditional sentence starts with “as soon as”, the begin
            // index shall point to the first “as”, so that it is clear that
            // this acceptance criterion also includes the words “as soon as”,
            // which must not be part of any expected result.
            beginIndex -= 3;
        } else {
            // If the conditional sentence starts with another conditional
            // starter word, the begin index shall point at it, so that it is
            // clear that this acceptance criterion also includes this
            // conditional starter word, which must not be part of any expected
            // result.
            beginIndex -= 1;
        }
        if (!verbInAction(actionWords, root, actionString)) {
            // If there is no verb in the action, we most likely have a
            // temporal relation like “after the import”, where we need to add
            // a verb in order to create an action acceptance criteria, as in
            // “[WHEN] the import HAPPENS”
            actionString += " happens";
        }
        if (conditionalStarterWord.index() < indexSoThat) {
            // If the action is located in the goal of the user story, we
            // simply add it to the list.
            acceptanceCriteria.add(new AcceptanceCriterion(actionString, AcceptanceCriterionType.ACTION, beginIndex, endIndex));
        } else {
            // If the action is located in the reason of the user story, we
            // assign the corresponding _IN_REASON type to it and also generate
            // an expected result from the remainder of the reason, which we
            // add to the list of acceptance criteria.
            acceptanceCriteria.add(new AcceptanceCriterion(actionString, AcceptanceCriterionType.ACTION_IN_REASON, beginIndex, endIndex));
            acceptanceCriteria.addAll(extractResultInformationInReason(sentence, userStoryString, endIndex));
        }
        return acceptanceCriteria;
    }

    /**
     * Determines the 1-based index of the first word of a duration identified
     * by the Named Entity Recognizer.
     * 
     * @param conditionalStarterWord the word succeeding the duration
     * expression
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return the 1-based index of the first word of the duration
     * 
     * @see ActionUtils#extractActionFromConditionalStarterWord
     */
    private static int getBeginIndexOfDuration(IndexedWord conditionalStarterWord, CoreSentence sentence) {
        // Remember that the indices of the nerTags() list are 0-based
        int indexOfDuration = conditionalStarterWord.index() - 1 - 1;
        while (sentence.nerTags().get(indexOfDuration - 1).equals("DURATION")) {
            indexOfDuration -= 1;
        }
        return indexOfDuration + 1;
    }


    /**
     * Determines the (presumed) existence of a verb among a set of words.
     * 
     * @param actionWords the set of words to investigate
     * @param root the root of a dependency graph subgraph
     * @param actionString the string containing an action acceptance criterion
     * @return {@code true} if a verb exists among the set of words or evidence
     * suggests that a verb has been cut off or has falsely been detected as a
     * plural noun
     * 
     * @see ActionUtils#extractActionFromConditionalStarterWord
     */
    private static boolean verbInAction(List<IndexedWord> actionWords, IndexedWord root, String actionString) {
        for (IndexedWord actionWord : actionWords) {
            if (actionWord.tag().startsWith("VB")) {
                return true;
            }
        }
        // If the action string ends with “[…]”, it is likely that the user
        // story was cut off at that location and there used to be a verb in
        // the action. Also, if the root word of the conditional sentence is
        // detected as a plural noun (NN[P]S), it is likely that the root is
        // actually a verb that has been falsely detected as a plural noun.
        return actionString.endsWith("[…]") || (root.tag().startsWith("NN") && root.tag().endsWith("S"));
    }

    /**
     * Determines the 1-based index of the last word of an action.
     * 
     * @param wordsInAction the list of words of the action
     * @param conditionalStarterWord the conditional starter word of the action
     * @param indexSoThat the 1-based index of the “so that” keyword of the
     * user story
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return the 1-based index of the last word of the action
     * 
     * @see ActionUtils#extractActionFromConditionalStarterWord
     */
    private static int determineActionEndIndex(List<IndexedWord> wordsInAction, IndexedWord conditionalStarterWord, int indexSoThat, CoreSentence sentence) {
        // We assume that all words in the list are actually part of the action
        int endIndex = wordsInAction.get(wordsInAction.size() - 1).index();
        if (conditionalStarterWord.index() < indexSoThat) {
            // If the “so that” keyword of the user story interrupts the action
            // words, the action ends there.
            endIndex = Math.min(endIndex, indexSoThat - 1);
        }

        // We iterate through the words and count the number of parentheses we
        // are currently in.
        int inParentheses = 0;
        for (IndexedWord wordInAction : wordsInAction) {
            if (wordInAction.tag().equals("-LRB-")) {
                // left parenthesis
                inParentheses += 1;
            } else if (wordInAction.tag().equals("-RRB-")) {
                // right parenthesis
                inParentheses -= 1;
            } else if ((wordInAction.word().equals(",") || (wordInAction.word().equalsIgnoreCase("that") && wordInAction.tag().equals("IN"))) && inParentheses == 0) {
                // If we reached a comma or a “that” preposition, it is likely
                // that we reached the end of the action. However, commas
                // inside parentheses often result from enumerations and do not
                // end the action.
                endIndex = Math.min(endIndex, wordInAction.index() - 1);
                break;
            }
        }
        
        // If an opening parenthesis follows the action, it is likely that it
        // belongs to the action. Therefore, we add the content of the
        // parenthesis to the action.
        if (endIndex < sentence.posTags().size() && sentence.posTags().get(endIndex - 1 + 1).equals("-LRB-")) {
            endIndex += 1;
            inParentheses += 1;
        }
        while (endIndex < sentence.posTags().size() && inParentheses > 0) {
            endIndex += 1;
            if (sentence.posTags().get(endIndex - 1).equals("-RRB-")) {
                // Here, we found the closing parenthesis.
                inParentheses -= 1;
            }
        }
        return endIndex;
    }

    /**
     * Extracts an expected result acceptance criterion from the reason of a
     * user story.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param userStoryString a string containing the user story
     * @param endIndexOfAction the 1-based index of the last word of the
     * preceeding action
     * @return a list of zero or one expected result acceptance criteria
     * 
     * @see ActionUtils#extractActionFromConditionalStarterWord
     */
    private static List<AcceptanceCriterion> extractResultInformationInReason(CoreSentence sentence, String userStoryString, int endIndexOfAction) {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();
        int wordsInSentenceCount = sentence.dependencyParse().getSubgraphVertices(sentence.dependencyParse().getFirstRoot()).size();
        
        // the expected result begins where the action ended
        int beginIndex = endIndexOfAction + 1;
        
        // the expected result contains all words after the action
        int endIndex = wordsInSentenceCount;

        if (sentence.dependencyParse().getNodeByIndex(beginIndex).word().equals(",")) {
            // if the action ended at a comma, that comma is not part of the
            // expected result
            beginIndex += 1;
        }
        if (sentence.dependencyParse().getNodeByIndex(endIndex).tag().equals(".")) {
            // the sentence period is not part of the expected result
            endIndex -= 1;
        }

        if (beginIndex <= endIndex) {
            // If there are words left after the end of the action, we
            // determine the 1-based indices of the first and last words and
            // then retrieve the begin and end position of these words in the
            // user story string. 
            int beginPosition = sentence.dependencyParse().getNodeByIndex(beginIndex).beginPosition();
            int endPosition = sentence.dependencyParse().getNodeByIndex(endIndex).endPosition();
            acceptanceCriteria.add(new AcceptanceCriterion(userStoryString.substring(beginPosition, endPosition), AcceptanceCriterionType.RESULT_IN_REASON, endIndexOfAction + 1, wordsInSentenceCount));
        }
        return acceptanceCriteria;
    }

    /**
     * Extracts an interaction from a user story and creates an action
     * acceptance criteria from it.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param userStoryString a string containing the user story
     * @return a list containing zero or one acceptance criteria with
     * actions from an interaction in the user story
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#extractActions
     */
    public static List<AcceptanceCriterion> extractActionFromInteraction(CoreSentence sentence, String userStoryString) {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();
        if (!userStoryString.toLowerCase().contains("to click")) {
            // If there is no interaction in the specified form, we cannot
            // continue and return the empty list.
            return acceptanceCriteria;
        }

        // Determine the start position of the interaction as the position of
        // the first character after the “to click” expression and a space.
        int beginPosition = userStoryString.toLowerCase().indexOf("to click") + "to click ".length();

        // The interaction ends in most cases after one of the words “to” or
        // “and”, the position of which we are determining next.
        int endPositionTo = userStoryString.toLowerCase().indexOf(" to ", beginPosition);
        int endPositionAnd = userStoryString.toLowerCase().indexOf(" and ", beginPosition);
        int endPosition = -1;
        if (endPositionTo == -1 ^ endPositionAnd == -1) {
            // If exactly one of the words was found, its position is larger
            // than the other position (which is -1), so we use the maximum of
            // both values
            endPosition = Math.max(endPositionTo, endPositionAnd);
        } else {
            // If both or no words were found, we use the minimum to make use
            // of the first word (if no words are found, both positions are -1)
            endPosition = Math.min(endPositionTo, endPositionAnd);
        }

        // If we found the end of the interaction
        if (endPosition != -1) {
            String acceptanceCriterionString = userStoryString.substring(beginPosition, endPosition);
            // In order to be able to remove the interaction information from
            // the expected result, we need to know the 1-based indices of the
            // first and last words that are included in the action acceptance
            // criterion.
            int beginIndex = getIndexFromPosition(sentence, beginPosition - "click ".length(), true);
            int endIndex = getIndexFromPosition(sentence, endPosition + " and".length(), false);

            // Remove commas and spaces from the end of the action
            while (acceptanceCriterionString.endsWith(",") || acceptanceCriterionString.endsWith(" ")) {
                acceptanceCriterionString = acceptanceCriterionString.substring(0, acceptanceCriterionString.length() - 1);
            }

            // The interaction “I want to be able to click on the button and
            // receive results” would be restructured into “WHEN the user
            // clicks on the button”.
            acceptanceCriteria.add(new AcceptanceCriterion("the user clicks " + acceptanceCriterionString, AcceptanceCriterionType.ACTION, beginIndex, endIndex));
        }
        return acceptanceCriteria;
    }

    /**
     * Determines the 1-based index of a word whose begin or end position is
     * identical to the position specified.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param position the begin or end position of a word in the user story
     * string
     * @param isBeginPosition whether the begin or end position is specified
     * @return the 1-based index of the word whose position is identical to the
     * position specified
     * 
     * @see ActionUtils#extractActionFromInteraction
     */
    private static int getIndexFromPosition(CoreSentence sentence, int position, boolean isBeginPosition) {
        Set<IndexedWord> wordsInSentence = sentence.dependencyParse().getSubgraphVertices(sentence.dependencyParse().getFirstRoot());
        int beginIndex = Integer.MAX_VALUE;
        int endIndex = 0;

        // Iterate over every word in the dependency graph
        for (IndexedWord word : wordsInSentence) {
            if (word.beginPosition() >= position) {
                // If the current word’s begin position is not before the
                // specified position and is also the first word with that
                // property, we remember its 1-based index.
                beginIndex = Math.min(beginIndex, word.index());
            }
            if (word.endPosition() <= position) {
                // If the current word’s end position is not after the
                // specified position and is also the last word with that
                // property, we remember its 1-based index.
                endIndex = Math.max(endIndex, word.index());
            }
        }

        // Return the index that was asked for
        return isBeginPosition ? beginIndex : endIndex;
    }

    /**
     * Determines the root node of a dependency graph subgraph which is likely
     * to contain all words of an action
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param conditionalStarterWord the conditional starter word of the action
     * @return the root node of a dependency graph subgraph which is likely to
     * contain all words of the action
     * 
     * @see ActionUtils#extractActionFromConditionalStarterWord
     */
    private static IndexedWord getRootOfAction(CoreSentence sentence, IndexedWord conditionalStarterWord) {
        IndexedWord parent = sentence.dependencyParse().getParent(conditionalStarterWord);
        if (parent.tag().startsWith("VB") && parent.index() > conditionalStarterWord.index()) {
            // If the parent of the conditional starter word is a verb which
            // lies after the conditional starter word, it is most likely the
            // root element we are looking for.
            return parent;
        }
        if (sentence.dependencyParse().getSubgraphVertices(conditionalStarterWord).size() > 1 || sentence.dependencyParse().getNodeByIndex(conditionalStarterWord.index() + 1).word().equalsIgnoreCase("necessary") || sentence.dependencyParse().getNodeByIndex(conditionalStarterWord.index() - 1).tag().equals("-LRB-") || sentence.nerTags().get(conditionalStarterWord.index() - 1).toUpperCase().startsWith("DATE")) {
            // If the conditional starter word’s subgraph itself contains more
            // than one element, it is our best guess for the root element of
            // the action word subgraph. Otherwise, our best guess is the
            // parent element, unless evidence suggests that the conditional
            // starter word does not start a conditional sentence which should
            // be converted into an action acceptance criterion at all. This is
            // the case if the conditional starter word (most likely “if”) is
            // followed by the word “necessary”, since “WHEN necessary” would
            // not be a very useful acceptance criterion. It is also the case
            // if the conditional starter word is preceeded by a parenthesis,
            // since usually those contain very short conditional sentences
            // such as “(if available)” or “(if not determinable)”. It is also
            // the case if the conditional starter word is part of a date
            // specification, as in “once a day”. In these cases, the
            // conditional starter word is returned as the root of a subgraph
            // which contains only one element, which will lead to no
            // acceptance criteria being created, as intended.
            return conditionalStarterWord;
        }
        return parent;
    }
    
}
