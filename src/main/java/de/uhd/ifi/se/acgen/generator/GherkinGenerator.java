package de.uhd.ifi.se.acgen.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.uhd.ifi.se.acgen.exception.SubjectNotFoundException;
import de.uhd.ifi.se.acgen.model.UserStory;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class GherkinGenerator implements Generator {
    
    public List<String> generate(UserStory userStory) throws SubjectNotFoundException {
        String userStoryString = userStory.getUserStoryString();
        List<String> acceptanceCriteria = new ArrayList<String>();
        userStoryString = preprocessing(userStoryString);
        acceptanceCriteria.add(userStoryString);
        return acceptanceCriteria;
    }

    private String preprocessing(String userStoryString) throws SubjectNotFoundException {
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

    private Map<Integer, String> switchToThirdPerson(CoreDocument document, CoreSentence userStorySentence) throws SubjectNotFoundException {
        Map<Integer, String> newReplacements = new HashMap<Integer, String>();
        IndexedWord subject = getSubject(userStorySentence);
        if (subject == null) {
            throw new SubjectNotFoundException("The subject of the user story could not be identified.");
        }
        List<IndexedWord> coreferencesOfSubject =  getCoreferencesOfWord(document, subject);
        if (coreferencesOfSubject.isEmpty()) {
            coreferencesOfSubject.add(subject);
        }
        for (IndexedWord coreferenceOfSubject : coreferencesOfSubject) {
            if (coreferenceOfSubject.word().equalsIgnoreCase("I")) {
                newReplacements.put(coreferenceOfSubject.index(), "the user");
                newReplacements.putAll(addSToVerbOfSubject(userStorySentence, subject));
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

    private IndexedWord getSubject(CoreSentence sentence) {
        IndexedWord root = sentence.dependencyParse().getFirstRoot();
        for (IndexedWord child : sentence.dependencyParse().getChildList(root)) {
            if (sentence.dependencyParse().getEdge(root, child).getRelation().getShortName().equals("nsubj") && child.tag().equals("PRP") && child.word().equalsIgnoreCase("I")) {
                return child;
            }
        }
        return null;
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
        if (parent.tag().equals("JJ")) {
            Set<IndexedWord> adjectiveChildren = userStorySentence.dependencyParse().getChildren(parent);
            for (IndexedWord adjectiveChild : adjectiveChildren) {
                if (userStorySentence.dependencyParse().getEdge(parent, adjectiveChild).getRelation().getShortName().equals("cop")) {
                    if (adjectiveChild.tag().equals("VBP")) {
                        newReplacements.put(adjectiveChild.index(), heSheItDasSMussMit(adjectiveChild.word()));
                    }
                    break;
                }
            }
        } else if (parent.tag().equals("VBP")) {
            newReplacements.put(parent.index(), heSheItDasSMussMit(parent.word()));
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
        if (verb.substring(verb.length() - 1).equals("x") || verb.substring(verb.length() - 2).equals("ss") || verb.substring(verb.length() - 2).equals("ch") || verb.substring(verb.length() - 2).equals("sh")) {
            return verb + "es";
        }
        
        if (verb.charAt(verb.length() - 1) == 'y' && !Arrays.asList('a', 'e', 'i', 'o', 'u').contains(verb.charAt(verb.length() - 2))) {
            return verb.substring(0, verb.length() - 1) + "ies";
        }
        return verb + "s";
    }

}
