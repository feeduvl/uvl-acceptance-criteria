package de.uhd.ifi.se.acgen.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.uhd.ifi.se.acgen.exception.MultipleSentencesException;
import de.uhd.ifi.se.acgen.model.UserStory;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.CorefChain.CorefMention;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;

public class GherkinGenerator implements Generator {
    
    public List<String> generate(UserStory userStory) throws MultipleSentencesException {
        String userStoryString = userStory.getUserStoryString();
        List<String> acceptanceCriteria = new ArrayList<String>();
        userStoryString = preprocessing(userStoryString);
        acceptanceCriteria.add(userStoryString);
        return acceptanceCriteria;
    }

    private String preprocessing(String userStoryString) throws MultipleSentencesException {
        StanfordCoreNLP pipeline = new StanfordCoreNLP(PropertiesUtils.asProperties("annotators", "tokenize,ssplit,pos,lemma,ner,depparse,coref"));
        CoreDocument document = new CoreDocument(userStoryString);
        pipeline.annotate(document);
        if (document.sentences().size() > 1) {
            throw new MultipleSentencesException("Multiple sentences have been detected.");
        }
        CoreSentence userStorySentence = document.sentences().get(0);

        IndexedWord subject = getSubject(userStorySentence);
        if (subject == null) {
            return userStoryString;
        }
        List<IndexedWord> coreferencesOfSubject =  getCoreferencesOfWord(document, subject);
        Map<Integer, String> replaceMap = new HashMap<Integer, String>();
        for (IndexedWord coreferenceOfSubject : coreferencesOfSubject) {
            if (coreferenceOfSubject.word().equalsIgnoreCase("I")) {
                replaceMap.put(coreferenceOfSubject.index(), "the user");
                IndexedWord parent = userStorySentence.dependencyParse().getParent(userStorySentence.dependencyParse().getNodeByIndex(coreferenceOfSubject.index()));
                if (parent.tag().equals("JJ")) {
                    Set<IndexedWord> adjectiveChildren = userStorySentence.dependencyParse().getChildren(parent);
                    for (IndexedWord adjectiveChild : adjectiveChildren) {
                        if (userStorySentence.dependencyParse().getEdge(parent, adjectiveChild).getRelation().getShortName().equals("cop")) {
                            if (adjectiveChild.tag().equals("VBP")) {
                                replaceMap.put(adjectiveChild.index(), heSheItDasSMussMit(adjectiveChild.word()));
                            }
                            break;
                        }
                    }
                } else if (parent.tag().equals("VBP")) {
                    replaceMap.put(parent.index(), heSheItDasSMussMit(parent.word()));
                }
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("me")) {
                replaceMap.put(coreferenceOfSubject.index(), "the user");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("my")) {
                replaceMap.put(coreferenceOfSubject.index(), "the user’s");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("mine")) {
                replaceMap.put(coreferenceOfSubject.index(), "the user’s");
            } else if (coreferenceOfSubject.word().equalsIgnoreCase("myself")) {
                replaceMap.put(coreferenceOfSubject.index(), "the user");
            }
        }

        for (CorefChain chain : document.annotation().get(CorefCoreAnnotations.CorefChainAnnotation.class).values()) {
            List<CorefMention> mentionsInChain = chain.getMentionsInTextualOrder();
            for (CorefMention mention : mentionsInChain) {
                if (replaceMap.containsKey(mention.headIndex)) {
                    continue;
                }
                if (userStorySentence.dependencyParse().getNodeByIndex(mention.headIndex).tag().equals("PRP")) {
                    replaceMap.put(mention.headIndex, chain.getRepresentativeMention().mentionSpan);
                } else if (userStorySentence.dependencyParse().getNodeByIndex(mention.headIndex).tag().equals("PRP$")) {
                    if (chain.getRepresentativeMention().mentionSpan.charAt(chain.getRepresentativeMention().mentionSpan.length() - 1) == 's') {
                        replaceMap.put(mention.headIndex, chain.getRepresentativeMention().mentionSpan + "’");
                    } else {
                        replaceMap.put(mention.headIndex, chain.getRepresentativeMention().mentionSpan + "’s");
                    }
                }
            }
        }

        return replaceWordsInSentence(userStorySentence, userStoryString, replaceMap);
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

    private String replaceWordsInSentence(CoreSentence sentence, String string, Map<Integer, String> replaceMap) {
        List<Integer> indicesOfWordsToBeReplaced = new ArrayList<Integer>(replaceMap.keySet());
        indicesOfWordsToBeReplaced.sort(Comparator.reverseOrder());
        for (int indexOfWordsToBeReplaced : indicesOfWordsToBeReplaced) {
            int startIndexInString = sentence.dependencyParse().getNodeByIndex(indexOfWordsToBeReplaced).beginPosition();
            int endIndexInString = sentence.dependencyParse().getNodeByIndex(indexOfWordsToBeReplaced).endPosition();
            string = string.substring(0, startIndexInString) + replaceMap.get(indexOfWordsToBeReplaced) + string.substring(endIndexInString);
        }
        return string;
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
