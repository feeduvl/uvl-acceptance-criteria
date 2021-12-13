package de.uhd.ifi.se.acgen.generator.gherkin.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.semgraph.SemanticGraph;

/**
 * Provides various utility functions for preprocessing of a user story.
 */
public final class PreprocessingUtils {

    /**
     * The private constructor of the utility class, preventing instantiation.
     */
    private PreprocessingUtils() {
        throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Converts a given sentence to third person.
     * 
     * @param document a {@code CoreDocument} containing the NLP analysis
     * result of the user story
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return a map which shows where to replace which words by which other
     * words in order to convert the user story to third person.
     * @throws TokenNotFoundException if an essential part of a user story such
     * as the verb or the subject could not be identified by the Stanford
     * CoreNLP tools.
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#preprocessing
     */
    public static Map<Integer, String> switchToThirdPerson(CoreDocument document, CoreSentence sentence) throws TokenNotFoundException {
        Map<Integer, String> newReplacements = new HashMap<Integer, String>();

        // Identify the subject of the sentence, which must be the “I” from the
        // “I want” expression of the user story.
        IndexedWord subject = identifySubject(sentence);

        // Using the Coreference annotator from Stanford CoreNLP, identify all
        // words (e.g. pronouns) that refer to the same entity as the subject
        // of the sentence (i.e., the “I” described above).
        List<IndexedWord> coreferencesOfSubject = getCoreferencesOfWord(document, subject);
        if (coreferencesOfSubject.isEmpty()) {
            // If no other references to the subject are found, add the subject
            // itself to the empty list so that it can be handled accordingly.
            coreferencesOfSubject.add(subject);
        }
        for (IndexedWord coreferenceOfSubject : coreferencesOfSubject) {
            // If the subject or one of its references is a known pronoun,
            // replace it with the third person expression “the user” in the
            // correct grammatical form
            if (coreferenceOfSubject.word().equalsIgnoreCase("I")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user");
                // If the word is a subject which has verbs associated to it,
                // the verbs must be converted to third person as well.
                newReplacements.putAll(addSToVerbOfSubject(sentence, coreferenceOfSubject));
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("me")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("my")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user’s");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("mine")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user’s");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("myself")) {
                newReplacements.put(coreferenceOfSubject.index(), "themself");
            }
        }
        return newReplacements;
    }

    /**
     * Adds a third-person s to the verb of a subject.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param subject the subject whose verbs are to be modified
     * @return a map which shows where to replace which words by which other
     * words in order to add the third-person s to the verb of the subject.
     * 
     * @see PreprocessingUtils#switchToThirdPerson
     */
    private static Map<Integer, String> addSToVerbOfSubject(CoreSentence sentence, IndexedWord subject) {
        Map<Integer, String> newReplacements = new HashMap<Integer, String>();

        // In many cases, the verb of the subject is its parent
        IndexedWord parent = sentence.dependencyParse().getParent(sentence.dependencyParse().getNodeByIndex(subject.index()));
        if (parent.tag().equals("VBP")) {
            // If the parent is indeed a third-person single present verb
            newReplacements.put(parent.index(), SharedUtils.heSheItDasSMussMit(parent.word()));
        } else if (parent.tag().equals("JJ") || parent.tag().startsWith("NN")) {
            // If the parent is an adjective or a noun or proper noun, we check
            // the children of the parent for third-person single present verbs
            // which are related to the parent by a copular relation.
            IndexedWord child = getFirstChildWithRelationAndTag(sentence.dependencyParse(), parent, "cop", "VBP");
            if (child != null) {
                newReplacements.put(child.index(), SharedUtils.heSheItDasSMussMit(child.word()));
            }
        } else if (parent.tag().equals("VB") || parent.tag().equals("VBN")) {
            // If the parent is a base-form verb or a past-participle verb, we
            // check the children of the parent for third-person single present
            // verbs which are related to the parent by an auxiliary relation.
            IndexedWord child = getFirstChildWithRelationAndTag(sentence.dependencyParse(), parent, "aux", "VBP");
            if (child != null) {
                newReplacements.put(child.index(), SharedUtils.heSheItDasSMussMit(child.word()));
            }
        }
        return newReplacements;
    }

    /**
     * Identifies the subject of the sentence, which must be the “I” from the
     * “I want” expression of the user story.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @return the subject of the sentence
     * @throws TokenNotFoundException if an essential part of a user story such
     * as the verb or the subject could not be identified by the Stanford
     * CoreNLP tools.
     * 
     * @see PreprocessingUtils#switchToThirdPerson
     */
    private static IndexedWord identifySubject(CoreSentence sentence) throws TokenNotFoundException {
        // Identify the subject of the sentence, which must be the “want” from the
        // “I want” expression of the user story.
        IndexedWord verb = SharedUtils.identifyVerb(sentence, false);

        // Check all words that are a child of the verb in the dependency graph
        for (IndexedWord child : sentence.dependencyParse().getChildList(verb)) {
            // The subject must be linked from the verb using the “nsubj”
            // (nominal subject) relation and must be a personal pronoun (PRP)
            // and must be “I”.
            if (sentence.dependencyParse().getEdge(verb, child).getRelation().getShortName().equals("nsubj") && child.tag().equals("PRP") && child.word().equalsIgnoreCase("I")) {
                return child;
            }
        }
        // If no such word exists, we cannot continue. Throw an appropriate
        // exception.
        throw new TokenNotFoundException("The subject of the user story could not be identified.");
    }

    /**
     * Identifies the first child of a word in a dependency graph, which has a
     * specified POS tag and is linked to its parent word via a specified
     * relation.
     * 
     * @param graph the dependency graph
     * @param parent the word which children are examined
     * @param relationShortName the short name of the grammatical relation
     * between the word and the child
     * @param tag the POS tag the child shall have
     * @return the first child of the given word with the specified relation
     * and POS tag, or {@code null}, if none exist.
     * 
     * @see PreprocessingUtils#addSToVerbOfSubject
     */
    private static IndexedWord getFirstChildWithRelationAndTag(SemanticGraph graph, IndexedWord parent, String relationShortName, String tag) {
        List<IndexedWord> children = graph.getChildList(parent);
        for (IndexedWord child : children) {
            if (graph.getEdge(parent, child).getRelation().getShortName().startsWith(relationShortName) && child.tag().equals(tag)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Finds all coreferences of a word, i.e., allwords (e.g. pronouns) that
     * refer to the same entity as the given word.
     * 
     * @param document a {@code CoreDocument} containing the NLP analysis
     * result of the user story
     * @param word the word of which coreferences are to be found
     * @return a list of words which are coreferences of the word we analyze
     * 
     * @see PreprocessingUtils#switchToThirdPerson
     */
    private static List<IndexedWord> getCoreferencesOfWord(CoreDocument document, IndexedWord word) {
        // We iterate through all coreference chains that have been detected
        for (CorefChain chain : document.annotation().get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            // In the current chain, we check whether it includes our word
            Set<CorefMention> mentionsWithWordAsHead = chain.getMentionsWithSameHead(1, word.index());
            if (mentionsWithWordAsHead != null) {
                // If there are coreferences of our word, we extract their
                // first word and return them. Since we are only interested
                // in (one-word) pronouns, we do not need to care about other
                // words being part of the mentions. 
                List<CorefMention> allMentionsOfWord = chain.getMentionsInTextualOrder();
                List<IndexedWord> coreferencesOfWord = new ArrayList<IndexedWord>();
                for (CorefMention mentionOfWord : allMentionsOfWord) {
                    coreferencesOfWord.add(document.sentences().get(0).dependencyParse().getNodeByIndex(mentionOfWord.headIndex));
                }
                return coreferencesOfWord;
            }
        }
        // If no coreferences are found, return an empty list.
        return new ArrayList<IndexedWord>();
    }

    /**
     * Replaces words in a sentence string by other words.
     * 
     * @param sentence a {@code CoreSentence} containing the NLP analysis
     * result of the user story
     * @param string the sentence string
     * @param replacements a map which shows where to replace which words by
     * which other words
     * @return the updated sentence string
     * 
     * @see de.uhd.ifi.se.acgen.generator.gherkin.GherkinGenerator#preprocessing
     */
    public static String replaceWordsInSentence(CoreSentence sentence, String string, Map<Integer, String> replacements) {
        List<Integer> indicesOfWordsToBeReplaced = new ArrayList<Integer>(replacements.keySet());
        String updatedString = string;

        // Iterate through the map entries in reverse order, so that previous
        // replacements do not affect the positions of later replacements in
        // the string.
        indicesOfWordsToBeReplaced.sort(Comparator.reverseOrder());
        for (int indexOfWordsToBeReplaced : indicesOfWordsToBeReplaced) {
            // Get the begin and end positions for the replacement from the
            // dependency graph nodes
            int startIndexInString = sentence.dependencyParse().getNodeByIndex(indexOfWordsToBeReplaced).beginPosition();
            int endIndexInString = sentence.dependencyParse().getNodeByIndex(indexOfWordsToBeReplaced).endPosition();

            // Cut the string at the begin and end position and replace the
            // middle part by the corresponding replacement string from the map
            updatedString = updatedString.substring(0, startIndexInString) + replacements.get(indexOfWordsToBeReplaced) + updatedString.substring(endIndexInString);
        }
        return updatedString;
    }

}
