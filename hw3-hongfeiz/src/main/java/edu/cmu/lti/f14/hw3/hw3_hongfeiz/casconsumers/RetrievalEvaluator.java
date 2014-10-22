package edu.cmu.lti.f14.hw3.hw3_hongfeiz.casconsumers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;  
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f14.hw3.hw3_hongfeiz.typesystems.Document;
import edu.cmu.lti.f14.hw3.hw3_hongfeiz.typesystems.Token;
import edu.cmu.lti.f14.hw3.hw3_hongfeiz.utils.Utils;


public class RetrievalEvaluator extends CasConsumer_ImplBase {

	/** query id number **/
	public ArrayList<Integer> qIdList;

	/** query and text relevant values **/
	public ArrayList<Integer> relList;

	public static final String PARAM_OUTPUTDIR = "OutputFile";
  
  private File outFile;
  
  FileOutputStream outStream;
  
  public ArrayList<Map<String, Integer>> tokenFrequency;

  public ArrayList<String> sentence;
  
	public void initialize() throws ResourceInitializationException {

	  outFile = new File(((String) getConfigParameterValue(PARAM_OUTPUTDIR)).trim());
    try { 
      if (outFile.exists()) {
        outFile.delete();
      }
      outFile.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      outStream = new FileOutputStream(outFile,true);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
		qIdList = new ArrayList<Integer>();

		relList = new ArrayList<Integer>();
		
		tokenFrequency = new ArrayList<Map<String, Integer>>();
		
		sentence = new ArrayList<String>();
	}

	/**
	 * 1. construct the global word dictionary 2. keep the word
	 * frequency for each sentence
	 */
	public void processCas(CAS aCas) throws ResourceProcessException {

		JCas jcas;
		try {
			jcas =aCas.getJCas();
		} catch (CASException e) {
			throw new ResourceProcessException(e);
		}

		FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();
	
		if (it.hasNext()) {
			Document doc = (Document) it.next();

			//Make sure that your previous annotators have populated this in CAS
			FSList fsTokenList = doc.getTokenList();
			//ArrayList<Token>tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);
			ArrayList<Token> tokens = Utils.fromFSListToCollection(fsTokenList, Token.class);
      Map<String, Integer> vector = new HashMap<String, Integer>();
      for (Token token : tokens) {
        vector.put(token.getText(), token.getFrequency());
      }
      tokenFrequency.add(vector);
      sentence.add(doc.getText());
			qIdList.add(doc.getQueryID());
			relList.add(doc.getRelevanceValue());
		}

	}

	/**
	 * 1. Compute Cosine Similarity and rank the retrieved sentences 2.
	 * Compute the MRR metric
	 */
	
 
  ArrayList<Integer> ranks = new ArrayList<Integer>();
	@Override
	public void collectionProcessComplete(ProcessTrace arg0)
			throws ResourceProcessException, IOException {

		super.collectionProcessComplete(arg0);
		
		// TODO :: compute the cosine similarity measure
		Map<Integer, Map<String, Integer>> queryMap = new HashMap<Integer, Map<String, Integer>>();
		ArrayList<Integer> queryId = new ArrayList<Integer>();
		Map<Integer, Double> similarMap = new HashMap<Integer, Double>();
		Map<Integer, Integer> answerMap = new HashMap<Integer, Integer>();
		for (int i = 0; i < qIdList.size(); i++) {
      int qid = qIdList.get(i);
      int rel = relList.get(i);
      Map<String, Integer> vector = tokenFrequency.get(i);
      if (rel == 99) {
        queryMap.put(qid, vector);
        queryId.add(qid);
      }
      else if (queryMap.containsKey(qid)){
        double similarity = computeCosineSimilarity(queryMap.get(qid),vector);
        similarMap.put(i, similarity);
        answerMap.put(i, qid);
      }
		}
		
  	for(int j = 0; j< queryId.size(); j++){
  	  int rank = 1;
  	  int qid = queryId.get(j);
  	  ArrayList<SortList> similarSort = new ArrayList<SortList>();
  		for (int i = 0; i < qIdList.size(); i++){
  		  if(answerMap.containsKey(i) && qid == answerMap.get(i)){
  		    SortList p = new SortList(i, qid,relList.get(i),similarMap.get(i));
  		    similarSort.add(p);
  		  }
  		}
  		Collections.sort(similarSort);
  		for (SortList t : similarSort) {
  		  if (t.rel == 1)
  		    break;
  		  rank++;
  		}
  		SortList sl = similarSort.get(rank-1);
  		int textPos = sl.getSId();
  		String line = "cosine=" + String.format("%.4f", sl.getSim()) + "\trank=" + rank + "\tqid="
  		        + qid + "\trel=1\t" + sentence.get(textPos) + '\n';
  		outStream.write(line.getBytes("UTF-8"));
  		ranks.add(rank);
		}
		
		double metric_mrr = compute_mrr();
		//System.out.println(" (MRR) Mean Reciprocal Rank ::" + String.format("%.4f", metric_mrr));
		String outSentence = " (MRR) Mean Reciprocal Rank ::" + String.format("%.4f", metric_mrr) + "\n";
		outStream.write(outSentence.getBytes("UTF-8"));
	}

	/**
	 * 
	 * @return cosine_similarity
	 */
	private double computeCosineSimilarity(Map<String, Integer> queryVector,
			Map<String, Integer> docVector) {
		double cosine_similarity=0.0;
		Iterator<String> queryIterator = queryVector.keySet().iterator();
		int numerator = 0;
    while (queryIterator.hasNext()) {
      String word = (String) queryIterator.next();
      if (docVector.containsKey(word)) {
        numerator += docVector.get(word) * queryVector.get(word);
      }
    }
    queryIterator = queryVector.keySet().iterator();
		int querySum = 0;
		while (queryIterator.hasNext()) {
  		String word = (String) queryIterator.next();
  		querySum += queryVector.get(word) * queryVector.get(word);
		}
		Iterator<String> docIterator = docVector.keySet().iterator();
		int docSum = 0;
		while (docIterator.hasNext()) {
  		String word = (String) docIterator.next();
  		docSum += docVector.get(word) * docVector.get(word);
		}
		cosine_similarity = (double) numerator / (Math.sqrt((double) querySum) * Math.sqrt((double) docSum));
		return cosine_similarity;
	}

	/**
	 * 
	 * @return mrr
	 */
	private double compute_mrr() {
		double metric_mrr=0.0;
		int q = ranks.size();
		for (int r : ranks) {
		metric_mrr += 1.0 / r;
		}
		metric_mrr /= (double) q;
		return metric_mrr;
		
	}
	@Override
	public void destroy(){
	  try {
      outStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
	}

}
