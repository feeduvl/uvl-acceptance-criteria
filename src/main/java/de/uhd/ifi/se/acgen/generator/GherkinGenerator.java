package de.uhd.ifi.se.acgen.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import de.uhd.ifi.se.acgen.model.UserStory;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;

public class GherkinGenerator implements Generator {
    
    public List<String> generate(UserStory userStory, boolean debug) throws TokenNotFoundException {
        String userStoryString = userStory.getUserStoryString();
        List<String> acceptanceCriteria = new ArrayList<String>();
        userStoryString = preprocessing(userStoryString);
        if (debug) {
            acceptanceCriteria.add("DEBUG: " + userStoryString);
        }

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,depparse,regexner");
        props.setProperty("ssplit.isOneSentence", "true");
        props.setProperty("regexner.mapping", "src/main/java/de/uhd/ifi/se/acgen/generator/regexner/ui-mapping.txt");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreDocument document = new CoreDocument(userStoryString);
        pipeline.annotate(document);
        CoreSentence userStorySentence = document.sentences().get(0);

        acceptanceCriteria.addAll(extractRoleInformation(userStorySentence, userStoryString));
        acceptanceCriteria.addAll(extractUIInformation(userStorySentence, userStoryString));

        return acceptanceCriteria;
    }

    private String preprocessing(String userStoryString) throws TokenNotFoundException {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,depparse,coref");
        props.setProperty("ssplit.isOneSentence", "true");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        CoreDocument document = new CoreDocument(userStoryString);
        pipeline.annotate(document);
        CoreSentence userStorySentence = document.sentences().get(0);

        Map<Integer, String> replacements = new HashMap<Integer, String>();

        replacements.putAll(switchToThirdPerson(document, userStorySentence));
        replacements.putAll(resolvePronouns(document, userStorySentence, replacements));

        return replaceWordsInSentence(userStorySentence, userStoryString, replacements);
    }

    private Map<Integer, String> switchToThirdPerson(CoreDocument document, CoreSentence userStorySentence) throws TokenNotFoundException {
        Map<Integer, String> newReplacements = new HashMap<Integer, String>();
        IndexedWord subject = getSubject(userStorySentence);
        if (subject == null) {
            throw new TokenNotFoundException("The subject of the user story could not be identified.");
        }
        List<IndexedWord> coreferencesOfSubject =  getCoreferencesOfWord(document, subject);
        if (coreferencesOfSubject.isEmpty()) {
            coreferencesOfSubject.add(subject);
        }
        for (IndexedWord coreferenceOfSubject : coreferencesOfSubject) {
            if (coreferenceOfSubject.word().equalsIgnoreCase("I")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user");
                newReplacements.putAll(addSToVerbOfSubject(userStorySentence, coreferenceOfSubject));
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("me")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("my")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user’s");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("mine")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user’s");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("myself")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user");
            }
        }
        return newReplacements;
    }

    private Map<Integer, String> resolvePronouns(CoreDocument document, CoreSentence userStorySentence, Map<Integer, String> replacements) {
        Map<Integer, String> newReplacements = new HashMap<Integer, String>();
        for (CorefChain chain : document.annotation().get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            List<CorefMention> mentionsInChain = chain.getMentionsInTextualOrder();
            for (CorefMention mention : mentionsInChain) {
                if (replacements.containsKey(mention.headIndex) || newReplacements.containsKey(mention.headIndex)) {
                    continue;
                }
                if (userStorySentence.dependencyParse().getNodeByIndex(mention.headIndex).tag().equals("PRP")) {
                    newReplacements.put(mention.headIndex, chain.getRepresentativeMention().mentionSpan);
                } else if (userStorySentence.dependencyParse().getNodeByIndex(mention.headIndex).tag().equals("PRP$")) {
                    if (chain.getRepresentativeMention().mentionSpan.charAt(chain.getRepresentativeMention().mentionSpan.length() - 1) == 's') {
                        newReplacements.put(mention.headIndex, chain.getRepresentativeMention().mentionSpan + "’");
                    } else {
                        newReplacements.put(mention.headIndex, chain.getRepresentativeMention().mentionSpan + "’s");
                    }
                }
            }
        }
        return newReplacements;
    }

    private IndexedWord getSubject(CoreSentence sentence) throws TokenNotFoundException {
        IndexedWord verb = getVerb(sentence, false);
        for (IndexedWord child : sentence.dependencyParse().getChildList(verb)) {
            if (sentence.dependencyParse().getEdge(verb, child).getRelation().getShortName().equals("nsubj") && child.tag().equals("PRP") && child.word().equalsIgnoreCase("I")) {
                return child;
            }
        }
        return null;
    }

    private IndexedWord getVerb(CoreSentence sentence, boolean isThirdPerson) throws TokenNotFoundException {
        String expectedVerb = isThirdPerson ? heSheItDasSMussMit("want") : "want";
        String expectedTag = isThirdPerson ? "VBZ" : "VBP";

        IndexedWord root = sentence.dependencyParse().getFirstRoot();
        if (root.word().equalsIgnoreCase(expectedVerb) && root.tag().equals(expectedTag)) {
            return root;
        }
        List<IndexedWord> possibleVerbs = sentence.dependencyParse().getAllNodesByPartOfSpeechPattern(expectedTag);
        possibleVerbs.removeIf(possibleVerb -> (!possibleVerb.word().equalsIgnoreCase(expectedVerb)));
        if (possibleVerbs.size() == 0) {
            throw new TokenNotFoundException("The verb of the user story could not be identified.");
        }
        possibleVerbs.sort((possibleVerb, otherPossibleVerb) -> (sentence.dependencyParse().getPathToRoot(possibleVerb).size() - sentence.dependencyParse().getPathToRoot(otherPossibleVerb).size()));
        return possibleVerbs.get(0);
    }

    private List<IndexedWord> getCoreferencesOfWord(CoreDocument document, IndexedWord word) {
        for (CorefChain chain : document.annotation().get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            Set<CorefMention> mentionsWithWordAsHead = chain.getMentionsWithSameHead(1, word.index());
            if (mentionsWithWordAsHead != null) {
                List<CorefMention> allMentionsOfWord = chain.getMentionsInTextualOrder();
                List<IndexedWord> coreferencesOfWord = new ArrayList<IndexedWord>();
                for (CorefMention mentionOfWord : allMentionsOfWord) {
                    coreferencesOfWord.add(document.sentences().get(0).dependencyParse().getNodeByIndex(mentionOfWord.headIndex));
                }
                return coreferencesOfWord;
            }
        }
        return new ArrayList<IndexedWord>();
    }

    private Map<Integer, String> addSToVerbOfSubject(CoreSentence userStorySentence, IndexedWord subject) {
        Map<Integer, String> newReplacements = new HashMap<Integer, String>();
        IndexedWord parent = userStorySentence.dependencyParse().getParent(userStorySentence.dependencyParse().getNodeByIndex(subject.index()));
        if (parent.tag().equals("VBP")) {
            newReplacements.put(parent.index(), heSheItDasSMussMit(parent.word()));
        } else if (parent.tag().equals("JJ") || parent.tag().startsWith("NN")) {
            IndexedWord child = getFirstChildWithRelationAndTag(userStorySentence.dependencyParse(), parent, "cop", "VBP");
            if (child != null) {
                newReplacements.put(child.index(), heSheItDasSMussMit(child.word()));
            }
        } else if (parent.tag().equals("VB") || parent.tag().equals("VBN")) {
            IndexedWord child = getFirstChildWithRelationAndTag(userStorySentence.dependencyParse(), parent, "aux", "VBP");
            if (child != null) {
                newReplacements.put(child.index(), heSheItDasSMussMit(child.word()));
            }
        }
        return newReplacements;
    }

    private String replaceWordsInSentence(CoreSentence sentence, String string, Map<Integer, String> replacements) {
        List<Integer> indicesOfWordsToBeReplaced = new ArrayList<Integer>(replacements.keySet());
        String updatedString = string;
        indicesOfWordsToBeReplaced.sort(Comparator.reverseOrder());
        for (int indexOfWordsToBeReplaced : indicesOfWordsToBeReplaced) {
            int startIndexInString = sentence.dependencyParse().getNodeByIndex(indexOfWordsToBeReplaced).beginPosition();
            int endIndexInString = sentence.dependencyParse().getNodeByIndex(indexOfWordsToBeReplaced).endPosition();
            updatedString = updatedString.substring(0, startIndexInString) + replacements.get(indexOfWordsToBeReplaced) + updatedString.substring(endIndexInString);
        }
        return updatedString;
    }

    private String heSheItDasSMussMit(String verb) {
        // implemented following https://www.gymglish.com/en/gymglish/english-grammar/the-s-in-the-third-person-singular-form
        if (verb.equals("am")) {
            return "is";
        } else if (verb.equals("have")) {
            return "has";
        } else if (verb.equals("do")) {
            return "does";
        } else if (verb.equals("go")) {
            return "goes";
        }
        if (verb.toLowerCase().endsWith("x") || verb.toLowerCase().endsWith("ss") || verb.toLowerCase().endsWith("ch") || verb.toLowerCase().endsWith("sh")) {
            return verb + "es";
        }
        
        if (verb.toLowerCase().endsWith("y") && !Arrays.asList('a', 'e', 'i', 'o', 'u').contains(verb.charAt(verb.length() - 2))) {
            return verb.substring(0, verb.length() - 1) + "ies";
        }
        return verb + "s";
    }

    private Set<String> extractRoleInformation(CoreSentence userStorySentence, String userStoryString) throws TokenNotFoundException {
        Set<String> acceptanceCriteria = new HashSet<String>();

        Set<IndexedWord> wordsInUserStorySentence = userStorySentence.dependencyParse().getSubgraphVertices(userStorySentence.dependencyParse().getFirstRoot());
        
        int indexAs = Integer.MAX_VALUE;
        int indexTheUserWants = 0;
        for (IndexedWord word : wordsInUserStorySentence) {
            if (word.word().equalsIgnoreCase("as") && word.tag().equals("IN")) {
                indexAs = Math.min(indexAs, word.index());
            } else if (word.word().equalsIgnoreCase("the") && word.tag().equals("DT")) {
                if (wordsInUserStorySentence.size() < word.index() + 2) {
                    continue;
                }
                IndexedWord expectedUser = userStorySentence.dependencyParse().getNodeByIndex(word.index() + 1);
                IndexedWord expectedWants = userStorySentence.dependencyParse().getNodeByIndex(word.index() + 2);
                if (expectedUser.word().equalsIgnoreCase("user") && expectedUser.tag().equals("NN") && expectedWants.word().equalsIgnoreCase("wants") && expectedWants.tag().equals("VBZ")) {
                    indexTheUserWants = Math.max(indexTheUserWants, word.index());
                }
            }
        }

        if (indexAs == Integer.MAX_VALUE || indexTheUserWants == 0) {
            acceptanceCriteria.add("WARNING: The role of the user story could not be identified.");
        } else {
            int beginIndex = userStorySentence.dependencyParse().getNodeByIndex(indexAs + 1).beginPosition();
            if (userStorySentence.dependencyParse().getNodeByIndex(indexTheUserWants - 1).tag().equals(",")) {
                indexTheUserWants -= 1;
            }
            int endIndex = userStorySentence.dependencyParse().getNodeByIndex(indexTheUserWants - 1).endPosition();
            acceptanceCriteria.add("GIVEN " + userStoryString.substring(beginIndex, endIndex) + " is using the software");
        }

        return acceptanceCriteria;
    }

    private IndexedWord getFirstChildWithRelationAndTag(SemanticGraph graph, IndexedWord parent, String relationShortName, String tag) {
        List<IndexedWord> children = graph.getChildList(parent);
        for (IndexedWord child : children) {
            if (graph.getEdge(parent, child).getRelation().getShortName().startsWith(relationShortName) && child.tag().equals(tag)) {
                return child;
            }
        }
        return null;
    }

    private Set<String> extractUIInformation(CoreSentence sentence, String userStoryString) {
        Set<String> acceptanceCriteria = new HashSet<String>();
        List<String> nerTags = sentence.nerTags();
        List<String> posTags = sentence.posTags();
        int beginIndex = Integer.MAX_VALUE;
        for (int i = 0; i < nerTags.size(); i++) {
            if (nerTags.get(i).equals("UI")) {
                beginIndex = i;
                break;
            }
        }
        if (beginIndex == Integer.MAX_VALUE) {
            return acceptanceCriteria;
        }
        int endIndex = 0;
        for (int i = beginIndex; i < nerTags.size(); i++) {
            if (!nerTags.get(i).equals("UI") && !posTags.get(i).startsWith("NN") && !posTags.get(i).equals(",")) {
                endIndex = i - 1;
                if (!sentence.tokensAsStrings().get(i).equals("under") && !sentence.tokensAsStrings().get(i).equals("via") && !(sentence.tokensAsStrings().get(i).equals("in") && nerTags.get(i + 1).equals("UI")) && !sentence.tokensAsStrings().get(i).equals("of") && !sentence.tokensAsStrings().get(i).equals("for") && !((sentence.tokensAsStrings().get(i - 1).equals("of") || sentence.tokensAsStrings().get(i - 1).equals("for")) && posTags.get(i).equals("DT"))) {
                    break;
                }
            }
        }
        if (endIndex == 0) {
            endIndex = nerTags.size() - 1;
        }
        if (posTags.get(endIndex).equals(",")) {
            endIndex -= 1;
        }
        int beginPosition = sentence.dependencyParse().getNodeByIndex(beginIndex + 1).beginPosition();
        beginPosition = userStoryString.indexOf("the", beginPosition);
        int endPosition = sentence.dependencyParse().getNodeByIndex(endIndex + 1).endPosition();
        acceptanceCriteria.add("GIVEN the active user interface is " + userStoryString.substring(beginPosition, endPosition));

        return acceptanceCriteria;
    }

}
