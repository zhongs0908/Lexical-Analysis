package edu.unm.lexer.description;

import edu.unm.lexer.fa.DFA;
import edu.unm.lexer.fa.FAUtils;
import edu.unm.lexer.fa.NFA;
import edu.unm.lexer.fa.State;
import edu.unm.lexer.regexps.RE;
import edu.unm.lexer.regexps.REFactory;
import edu.unm.lexer.regexps.RENode;
import edu.unm.lexer.regexps.REUtils;

import java.util.*;

public class NFADescription extends Description {
    TokenClass identifier;
    TokenClass symbols;

    public NFADescription() {
        List<String> keywordsList = Arrays.asList(new String[]{"nfa", "end", "states", "initial", "accept", "transitions", "-->"});
        this.keyWords = new TokenClass(TokenUtils.keyWords, createKeywordsRE(keywordsList));
        this.identifier = new TokenClass(TokenUtils.identifier, createIdentifierRE());
        this.symbols = new TokenClass(TokenUtils.symbols, createSymbolRE());

        tokenClasses.add(keyWords);
        tokenClasses.add(identifier);
        tokenClasses.add(symbols);

        for (String s : keywordsList) {
            Token tmp = new Token(this.keyWords, s);
            this.keywordsTokenMap.put(s, tmp);
        }
    }

    private RE createIdentifierRE() {
        //non-empty strings of letters and digits
        RE result = REFactory.alterRE(REUtils.generateDigitsRE(), REUtils.generateLetterRE());
        result = REFactory.concatRE(result, REFactory.starRE(result));
        return result;
    }

    public RE createSymbolRE() {
        //'_  empty
        //''_ underscore
        //result 'ascii | ''_
        RE printableAscii = REFactory.generateASCiiRE();
        RE underScore = REFactory.concatRE(new RE("''", new RENode("''")), new RE("'_", new RENode("_")));
        RE result = REFactory.concatRE(new RE("''", new RENode("''")),  REFactory.alterRE(underScore, printableAscii));
        return result;
    }

    public NFA parse(Set<String> alphabet, List<Token> tokenList) {
        List<Token> notInAlphabetList = isInAlphabet(alphabet, tokenList);
        if (notInAlphabetList.size() > 0) {
            for (Token token : notInAlphabetList) {
                System.out.println("parsing error " + token.value + " is not in the alphabet");
            }
            return null;
        }

        Map<String, State> stateMap = new HashMap<>();
        State initState= null;
        Token endToken = this.keywordsTokenMap.get("end");
        Set<State> acceptStates = new HashSet<>();

        for (int i = 0; i < tokenList.size(); i++) {
            if (tokenList.get(i).equals(this.keywordsTokenMap.get("states"))) {
                i = parseStateList(tokenList, i+1, stateMap, endToken);
            } else if (tokenList.get(i).equals(this.keywordsTokenMap.get("initial"))) {
                initState = parseInitialState(tokenList, i + 1, stateMap);
            } else if (tokenList.get(i).equals(this.keywordsTokenMap.get("accept"))) {
                i = parseAcceptList(tokenList, i+ 1, acceptStates, stateMap, endToken);
            } else if (tokenList.get(i).equals(this.keywordsTokenMap.get("transitions"))) {
                i = parseTransitions(tokenList, i+ 1, stateMap, endToken, this.keywordsTokenMap.get("-->"));
            }

        }

        List<State> data = new ArrayList<>();
        for (Map.Entry<String, State> entry : stateMap.entrySet()) {
            data.add(entry.getValue());
        }
        return new NFA(data, initState, acceptStates, alphabet);
    }
    private List<Token> isInAlphabet(Set<String> alphabet, List<Token> tokenList) {
        //default we treat symbol '_ which is empty is in alphabet
        List<Token> result = new ArrayList<>();
        for (Token token : tokenList) {
            if (token.tokenClass.equals(symbols)) {
                if (!alphabet.contains(token.value)) {
                    //if (!token.value.equals("'_")) {
                        result.add(token);
                   // }
                }
            }
        }
        return result;
    }
    private Integer parseStateList(List<Token> tokenList, int start, Map<String, State> stateMap, Token endToken) {
        //return the index of end

        while (start < tokenList.size() && !tokenList.get(start).equals(endToken)) {
            String name = tokenList.get(start).value;
            stateMap.put(name, new State(name));
            start++;
        }

        return start;
    }

    private State parseInitialState(List<Token> tokenList, int start, Map<String, State> stateMap) {
        //return the initState
        return stateMap.get(tokenList.get(start).value);
    }
    private Integer parseAcceptList(List<Token> tokenList, int start, Set<State> acceptStates, Map<String, State> stateMap, Token endToken) {
        //return the index of end
        while (start < tokenList.size() && !tokenList.get(start).equals(endToken)) {
            acceptStates.add(stateMap.get(tokenList.get(start).value));
            start++;
        }
        return start;
    }
    private Integer parseTransitions(List<Token> tokenList, int start, Map<String, State> stateMap, Token endToken, Token arrow) {
        //return the index of end
        while (start < tokenList.size() && !tokenList.get(start).equals(endToken)) {
            State s = stateMap.get(tokenList.get(start).value);
            List<String> tmp = new ArrayList<>();
            start++;
            while (!tokenList.get(start).equals(arrow)) {
                tmp.add(tokenList.get(start).value);
                start++;
            }
            //now start is index for -->
            start++; //start is now endState
            State end = stateMap.get(tokenList.get(start).value);
            if (tmp.size() == 0) {
                FAUtils.createConnection(s, end, "");
            } else {
                for (String condition : tmp) {
                    if (condition.equals("'_")) {
                        FAUtils.createConnection(s, end, "");
                    } else {
                        FAUtils.createConnection(s, end, "" + condition.charAt(1));
                    }
                }
            }
            start++;
        }
        return start;
    }
}
