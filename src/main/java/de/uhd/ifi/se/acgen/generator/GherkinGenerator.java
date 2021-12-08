package de.uhd.ifi.se.acgen.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.uhd.ifi.se.acgen.exception.TokenNotFoundException;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterion;
import de.uhd.ifi.se.acgen.model.AcceptanceCriterionType;
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
    
    public List<AcceptanceCriterion> generate(UserStory userStory, boolean debug) throws TokenNotFoundException {
        String userStoryString = userStory.getUserStoryString();
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();
        userStoryString = preprocessing(userStoryString);
        if (debug) {
            acceptanceCriteria.add(new AcceptanceCriterion(userStoryString, AcceptanceCriterionType.DEBUG));
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
        acceptanceCriteria.addAll(extractConditionalInformation(userStorySentence, userStoryString));

        Collections.sort(acceptanceCriteria);
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

    private List<AcceptanceCriterion> extractRoleInformation(CoreSentence userStorySentence, String userStoryString) throws TokenNotFoundException {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();

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
                    break;
                }
            }
        }

        if (indexAs == Integer.MAX_VALUE || indexTheUserWants == 0) {
            acceptanceCriteria.add(new AcceptanceCriterion("The role of the user story could not be identified.", AcceptanceCriterionType.WARNING));
        } else {
            int beginIndex = userStorySentence.dependencyParse().getNodeByIndex(indexAs + 1).beginPosition();
            if (userStorySentence.dependencyParse().getNodeByIndex(indexTheUserWants - 1).tag().equals(",")) {
                indexTheUserWants -= 1;
            }
            int endIndex = userStorySentence.dependencyParse().getNodeByIndex(indexTheUserWants - 1).endPosition();
            acceptanceCriteria.add(new AcceptanceCriterion(userStoryString.substring(beginIndex, endIndex), AcceptanceCriterionType.ROLE));
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

    private List<AcceptanceCriterion> extractUIInformation(CoreSentence sentence, String userStoryString) {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();
        List<String> nerTags = sentence.nerTags();
        List<String> posTags = sentence.posTags();
        List<String> tokensAsStrings = sentence.tokensAsStrings();
        int beginIndex = Integer.MAX_VALUE;
        for (int i = 0; i < nerTags.size() - 1; i++) {
            if (nerTags.get(i).equals("UI")) {
                beginIndex = i;
                break;
            }
            if (tokensAsStrings.get(i).equalsIgnoreCase("so") && tokensAsStrings.get(i + 1).equalsIgnoreCase("that")) {
                return acceptanceCriteria;
            }
        }
        if (beginIndex == Integer.MAX_VALUE) {
            return acceptanceCriteria;
        }
        int endIndex = getEndIndexOfUI(beginIndex, sentence);
        if (endIndex == 0 || endIndex == nerTags.size()) {
            endIndex = nerTags.size() - 1;
        }
        if (posTags.get(endIndex).equals(",") || posTags.get(endIndex).equals("HYPH")) {
            endIndex -= 1;
        }
        int beginPosition = sentence.dependencyParse().getNodeByIndex(beginIndex + 1).beginPosition();
        beginPosition = userStoryString.indexOf("the", beginPosition);
        int endPosition = sentence.dependencyParse().getNodeByIndex(endIndex + 1).endPosition();
        acceptanceCriteria.add(new AcceptanceCriterion(userStoryString.substring(beginPosition, endPosition), AcceptanceCriterionType.UI));

        return acceptanceCriteria;
    }

    private int getEndIndexOfUI(int beginIndex, CoreSentence sentence) {
        List<String> nerTags = sentence.nerTags();
        List<String> posTags = sentence.posTags();
        List<String> tokensAsStrings = sentence.tokensAsStrings();
        int endIndex = 0;
        for (int i = beginIndex; i < nerTags.size(); i++) {
            if (i < endIndex) {
                continue;
            }
            if (!nerTags.get(i).equals("UI") && !posTags.get(i).startsWith("NN") && !posTags.get(i).equals(",") && !posTags.get(i).equals("HYPH")) {
                endIndex = i - 1;
                List<String> prepositions = Arrays.asList("under", "of", "for");
                if (prepositions.contains(tokensAsStrings.get(i).toLowerCase()) && sentence.dependencyParse().getParent(sentence.dependencyParse().getNodeByIndex(i + 1)).tag().startsWith("NN")) {
                    if (tokensAsStrings.get(i).equals("for") && posTags.get(i + 1).equals("VBG")) {
                        break;
                    }
                    endIndex = sentence.dependencyParse().getParent(sentence.dependencyParse().getNodeByIndex(i + 1)).index() - 1;
                } else if (tokensAsStrings.get(i).equals("in") && (nerTags.get(i + 1).equals("UI") || tokensAsStrings.get(i + 2).equalsIgnoreCase("list") || tokensAsStrings.get(i + 1).equalsIgnoreCase("CoMET"))) {
                    endIndex = i + 2;
                } else if (posTags.get(i).equals("``") && posTags.subList(i + 1, posTags.size()).contains("''")) {
                    endIndex = i + 2 + posTags.subList(i + 1, posTags.size()).indexOf("''");
                } else if (posTags.get(i).equals("-LRB-") && posTags.subList(i + 1, posTags.size()).contains("-RRB-")) {
                    endIndex = i + 2 + posTags.subList(i + 1, posTags.size()).indexOf("-RRB-");
                } else if (tokensAsStrings.get(i).equals("\"") && tokensAsStrings.subList(i + 1, tokensAsStrings.size()).contains("\"")) {
                    endIndex = i + 2 + tokensAsStrings.subList(i + 1, tokensAsStrings.size()).indexOf("\"");
                } else if (tokensAsStrings.get(i).equals(">") && (posTags.get(i - 1).equals("HYPH") || tokensAsStrings.get(i - 1).equals("-"))) {
                    endIndex = i + 1;
                } else {
                    break;
                }
            }
        }
        return endIndex;
    }

    private List<AcceptanceCriterion> extractConditionalInformation(CoreSentence sentence, String userStoryString) {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();
        List<String> tokensAsStrings = sentence.tokensAsStrings();
        int indexSoThat = tokensAsStrings.size() + 1;
        for (int i = 1; i < tokensAsStrings.size(); i++) {
            if (tokensAsStrings.get(i - 1).equalsIgnoreCase("so") && tokensAsStrings.get(i).equalsIgnoreCase("that")) {
                indexSoThat = i;
                break;
            }
        }

        List<String> conditionalStarterStrings = Arrays.asList("if", "when", "once", "whenever", "after");
        List<String> conditionalLimiterStrings = Arrays.asList("also", "even", "especially");

        List<IndexedWord> conditionalStarterWords = new ArrayList<IndexedWord>();

        for (int i = 3; i < indexSoThat; i++) {
            IndexedWord word = sentence.dependencyParse().getNodeByIndex(i);
            if ((conditionalStarterStrings.contains(word.word().toLowerCase()) || isAsSoonAs(sentence, i)) && !conditionalLimiterStrings.contains(sentence.dependencyParse().getNodeByIndex(i - 1).word().toLowerCase()) && !sentence.dependencyParse().getNodeByIndex(i + 1).word().equalsIgnoreCase("and")) {
                conditionalStarterWords.add(sentence.dependencyParse().getNodeByIndex(i));
            }
        }

        if (indexSoThat + 2 < tokensAsStrings.size() && conditionalStarterStrings.contains(sentence.dependencyParse().getNodeByIndex(indexSoThat + 2).word().toLowerCase())) {
            conditionalStarterWords.add(sentence.dependencyParse().getNodeByIndex(indexSoThat + 2));
        }

        for (IndexedWord conditionalStarterWord : conditionalStarterWords) {
            acceptanceCriteria.addAll(extractCauseInformationFromConditionalStarterWord(sentence, userStoryString, conditionalStarterWord, indexSoThat));
        }

        acceptanceCriteria.addAll(extractConditionalInformationFromInteraction(userStoryString));

        return acceptanceCriteria;
    }

    private boolean isAsSoonAs(CoreSentence sentence, int i) {
        return sentence.dependencyParse().getNodeByIndex(i).word().equalsIgnoreCase("as") && sentence.dependencyParse().getNodeByIndex(i - 1).word().equalsIgnoreCase("soon") && sentence.dependencyParse().getNodeByIndex(i - 2).word().equalsIgnoreCase("as");
    }

    private List<AcceptanceCriterion> extractCauseInformationFromConditionalStarterWord(CoreSentence sentence, String userStoryString, IndexedWord conditionalStarterWord, int indexSoThat) {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();
        IndexedWord root = getRootOfCondition(sentence, conditionalStarterWord);
        List<IndexedWord> conditionWords = new ArrayList<IndexedWord>();
        conditionWords.addAll(sentence.dependencyParse().getSubgraphVertices(root));
        if (conditionWords.size() < 2) {
            return acceptanceCriteria;
        }
        conditionWords.sort((word, otherWord) -> word.index() - otherWord.index());
        conditionWords.removeIf(word -> word.index() < conditionalStarterWord.index());
        int beginIndex = conditionalStarterWord.index() + 1;
        int endIndex = conditionWords.get(conditionWords.size() - 1).index();
        if (conditionalStarterWord.index() < indexSoThat) {
            endIndex = Math.min(endIndex, indexSoThat - 1);
        }
        int inParentheses = 0;
        for (IndexedWord conditionWord : conditionWords) {
            if (conditionWord.tag().equals("-LRB-")) {
                inParentheses += 1;
            } else if (conditionWord.tag().equals("-RRB-")) {
                inParentheses -= 1;
            } else if (conditionWord.word().equals(",") && inParentheses == 0) {
                endIndex = Math.min(endIndex, conditionWord.index() - 1);
                break;
            }
        }
        int wordsInSentenceCount = sentence.dependencyParse().getSubgraphVertices(sentence.dependencyParse().getFirstRoot()).size();
        if (conditionalStarterWord.index() > indexSoThat && endIndex >= wordsInSentenceCount - 1) {
            return acceptanceCriteria;
        }
        int beginPosition = sentence.dependencyParse().getNodeByIndex(beginIndex).beginPosition();
        int endPosition = sentence.dependencyParse().getNodeByIndex(endIndex).endPosition();
        if (conditionalStarterWord.index() < indexSoThat) {
            acceptanceCriteria.add(new AcceptanceCriterion(userStoryString.substring(beginPosition, endPosition), AcceptanceCriterionType.CAUSE));
        } else {
            acceptanceCriteria.add(new AcceptanceCriterion(userStoryString.substring(beginPosition, endPosition), AcceptanceCriterionType.CAUSE_IN_REASON));
            acceptanceCriteria.addAll(extractEffectInformationFromConditionalStarterWordInReason(sentence, userStoryString, endIndex));
        }
        return acceptanceCriteria;
    }

    private List<AcceptanceCriterion> extractEffectInformationFromConditionalStarterWordInReason(CoreSentence sentence, String userStoryString, int endIndexOfCause) {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();
        int wordsInSentenceCount = sentence.dependencyParse().getSubgraphVertices(sentence.dependencyParse().getFirstRoot()).size();
        int beginIndex = endIndexOfCause + 1;
        int endIndex = wordsInSentenceCount;
        if (sentence.dependencyParse().getNodeByIndex(beginIndex).word().equals(",")) {
            beginIndex += 1;
        }
        if (sentence.dependencyParse().getNodeByIndex(endIndex).tag().equals(".")) {
            endIndex -= 1;
        }
        if (beginIndex <= endIndex) {
            int beginPosition = sentence.dependencyParse().getNodeByIndex(beginIndex).beginPosition();
            int endPosition = sentence.dependencyParse().getNodeByIndex(endIndex).endPosition();
            acceptanceCriteria.add(new AcceptanceCriterion(userStoryString.substring(beginPosition, endPosition), AcceptanceCriterionType.EFFECT_IN_REASON));
        }
        return acceptanceCriteria;
    }

    private List<AcceptanceCriterion> extractConditionalInformationFromInteraction(String userStoryString) {
        List<AcceptanceCriterion> acceptanceCriteria = new ArrayList<AcceptanceCriterion>();
        if (!userStoryString.toLowerCase().contains("to click")) {
            return acceptanceCriteria;
        }
        int beginPosition = userStoryString.toLowerCase().indexOf("to click") + "to click ".length();
        int endPositionTo = userStoryString.toLowerCase().indexOf(" to ", beginPosition);
        int endPositionAnd = userStoryString.toLowerCase().indexOf(" and ", beginPosition);
        int endPosition = -1;
        if (endPositionTo == -1 ^ endPositionAnd == -1) {
            endPosition = Math.max(endPositionTo, endPositionAnd);
        } else {
            endPosition = Math.min(endPositionTo, endPositionAnd);
        }
        if (endPosition != -1) {
            String acceptanceCriterionString = userStoryString.substring(beginPosition, endPosition);
            while (acceptanceCriterionString.charAt(acceptanceCriterionString.length() - 1) == ',' || acceptanceCriterionString.charAt(acceptanceCriterionString.length() - 1) == ' ') {
                acceptanceCriterionString = acceptanceCriterionString.substring(0, acceptanceCriterionString.length() - 1);
            }
            acceptanceCriteria.add(new AcceptanceCriterion("the user clicks " + acceptanceCriterionString, AcceptanceCriterionType.CAUSE));
        }
        return acceptanceCriteria;
    }

    private IndexedWord getRootOfCondition(CoreSentence sentence, IndexedWord conditionalStarterWord) {
        IndexedWord parent = sentence.dependencyParse().getParent(conditionalStarterWord);
        if (parent.tag().startsWith("VB") && parent.index() > conditionalStarterWord.index()) {
            return parent;
        }
        return conditionalStarterWord;
    }

}
