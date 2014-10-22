package edu.cmu.lti.f14.hw3.hw3_hongfeiz.annotators;

import java.util.*;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;

import edu.cmu.lti.f14.hw3.hw3_hongfeiz.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_hongfeiz.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_hongfeiz.utils.Utils;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
		if (iter.isValid()) {
			iter.moveToNext();
			Document doc = (Document) iter.get();
			createTermFreqVector(jcas, doc);
		}

	}

	/**
   * A basic white-space tokenizer, it deliberately does not split on punctuation!
   *
	 * @param doc input text
	 * @return    a list of tokens.
	 */

	List<String> tokenize0(String doc) {
	  List<String> res = new ArrayList<String>();
	  
	  for (String s: doc.split("\\s+"))
	    res.add(s);
	  return res;
	}

	/**
	 * 
	 * @param jcas
	 * @param doc
	 */

	private void createTermFreqVector(JCas jcas, Document doc) {

		String docText = doc.getText();
		List<String> tokenList = tokenize0(docText);
		List<Token> tokens = new ArrayList<Token>();
    Map<String, Integer> tokenFrequency = new HashMap<String, Integer>();
    //Use a hashmap to store the frequency of token
    for (String token : tokenList) {
      if (!tokenFrequency.containsKey(token))
        tokenFrequency.put(token, 1);
      else
        tokenFrequency.put(token, tokenFrequency.get(token) + 1);
    }
    //extract <token,frequency> pairs from hashmap
    for (String token : tokenFrequency.keySet()) {
      Token tk = new Token(jcas);
      tk.setText(token);
      tk.setFrequency(tokenFrequency.get(token));
      tokens.add(tk);
    }
    FSList fs = Utils.fromCollectionToFSList(jcas, tokens);
    doc.setTokenList(fs);
	}

}
