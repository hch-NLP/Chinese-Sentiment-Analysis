package sentiment_analysis;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

//-------------------------------------------------------------------------------------
//Created by Chun_hui  HE, in NUDT .
//Copyright (C) 2018年10月11日 . All rights reserved.
//-------------------------------------------------------------------------------------

public class SentimentCalculate {
	private final static int MaxNum_Sentences=100;
	private final static String pospath="F:/eclipse/data/hanlp_sentimentanalysis/resoueces/positive.txt";
	private final static String nagpath="F:/eclipse/data/hanlp_sentimentanalysis/resoueces/nagetive.txt";
	private final static String degpath="F:/eclipse/data/hanlp_sentimentanalysis/resoueces/degree.txt";
	private final static String opppath="F:/eclipse/data/hanlp_sentimentanalysis/resoueces/opposite.txt";
	private final static String testdocpath="F:/eclipse/data/hanlp_sentimentanalysis/resoueces/test.txt";
	private final static Segment segment = HanLP.newSegment().enableCustomDictionary(true);
	public static void main(String[] args) throws Exception {
		Date starttime = new Date();
		ArrayList<String> testdocuments = new ArrayList<String>();
		FileInputStream fis = new FileInputStream(SentimentCalculate.testdocpath);
		InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		String str = null;
		while ((str = br.readLine()) != null) {
			testdocuments.add(str);
		}
		fis.close();
		isr.close();
		br.close();
		for (String content : testdocuments) {
			System.out.println("<**********************************>");
			SentimentCalculate sc = new SentimentCalculate();
			Map<String, String> calculateresult = sc.sentimentanalysis(content);
			//System.out.println(calculateresult.toString());
			System.out.println("情感得分：" + calculateresult.get("score"));
			System.out.println("正面情感词：" + calculateresult.get("poswords"));
			System.out.println("负面情感词：" + calculateresult.get("nagwords"));
		}
		Date endtime = new Date();
		System.out.println("总共耗时：" + (endtime.getTime() - starttime.getTime()) / 1000.0 + " (S)");
	}
	public Map<String, String> sentimentanalysis(String text) {
		try {
			final  ArrayList<String> positivewords = getPositveWords(SentimentCalculate.pospath);
			final  ArrayList<String> nagetivewords = getNagtiveWords(SentimentCalculate.nagpath);
			final  ArrayList<String> Degreewords = getDegree(SentimentCalculate.degpath);
			final  ArrayList<String> Oppositewords = getOppositeWords(SentimentCalculate.opppath);
			Map<String, String> map = new HashMap<String, String>();
			final HashSet<String> positivewordlist = new HashSet<String>();
			final HashSet<String> nagetivewordlist = new HashSet<String>();
			double result;
			result = getScore(text, positivewords, nagetivewords, Degreewords, Oppositewords, positivewordlist,
					nagetivewordlist);
			map.put("score", String.valueOf(result));
			map.put("poswords", positivewordlist.toString());
			map.put("nagwords", nagetivewordlist.toString());
			positivewordlist.clear();
			nagetivewordlist.clear();
			positivewords.clear();
			nagetivewords.clear();
			Degreewords.clear();
			Oppositewords.clear();
			return map;
		} catch (Exception e) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("score", String.valueOf(0));
			map.put("poswords", "");
			map.put("nagwords", "");
			return map;
		}
	}

	public static double getScore(String text, ArrayList<String> positivewords, ArrayList<String> nagetivewords,
			ArrayList<String> Degreewords, ArrayList<String> Oppositewords, HashSet<String> positivewordlist,
			HashSet<String> nagetivewordlist) throws Exception {
		double result = 0;
		String[] sentences = getSentences(text);
		for (int i = 0; i < Math.min(SentimentCalculate.MaxNum_Sentences, sentences.length); i++) {
			result = result + score(sentences[i], positivewords, nagetivewords, Degreewords, Oppositewords,
					positivewordlist, nagetivewordlist);
		}
		return result;
	}

	public static String[] getSentences(String text) {
		return text.trim().split("[\uff0c|\u3002|\uff1b]");
	}

	public static double score(String sentence, ArrayList<String> positivewords, ArrayList<String> nagetivewords,
			ArrayList<String> Degreewords, ArrayList<String> Oppositewords, HashSet<String> positivewordlist,
			HashSet<String> nagetivewordlist) throws Exception {
		int opposite = 1;
		int Degreeword = 1;
		double score = 0.0;
		int lastindex = 0;
		if (sentence.length() <= 0) {
			return 0;
		}
		List<Term> termList = SentimentCalculate.segment.seg(sentence);
		String[] words = new String[termList.size() + 1];
		int index = 0;
		for (Term token : termList) {
			words[index] = token.word;
			index++;
		}
		for (int i = 0; i < index + 1; i++) {
			double k = isSentiment(words[i], positivewords, nagetivewords);
			if (k == 1.0 || k == -1.5) {
				if (k == 1.0 && words[i].length()>1) {
					positivewordlist.add(words[i]);
				}
				if (k == -1.5 && words[i].length()>1) {
					nagetivewordlist.add(words[i]);
				}
				for (int j = lastindex; j < i; j++) {
					if (isDegree(words[j], Degreewords)) {
						Degreeword = Degreeword * 2;
					}
					if (isOpposite(words[j], Oppositewords)) {
						opposite = opposite * (-1);
					}
				}
				score = score + sentimentScore(k, Degreeword, opposite);
				Degreeword = 1;
				opposite = 1;
				lastindex = i;
			}
		}
		return score;
	}

	public static ArrayList<String> getPositveWords(String path) {
		try {
			ArrayList<String> result = new ArrayList<String>();
			FileInputStream fis = new FileInputStream(path);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			String str = null;
			while ((str = br.readLine()) != null) {
				result.add(str);
			}
			fis.close();
			isr.close();
			br.close();
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	public static ArrayList<String> getNagtiveWords(String path) {
		try {
			ArrayList<String> result = new ArrayList<String>();
			FileInputStream fis = new FileInputStream(path);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			String str = null;
			while ((str = br.readLine()) != null) {
				result.add(str);
			}
			fis.close();
			isr.close();
			br.close();
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	public static ArrayList<String> getOppositeWords(String path) {
		try {
			ArrayList<String> result = new ArrayList<String>();
			FileInputStream fis = new FileInputStream(path);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			String str = null;
			while ((str = br.readLine()) != null) {
				result.add(str);
			}
			fis.close();
			isr.close();
			br.close();
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	public static ArrayList<String> getDegree(String path) {
		try {
			ArrayList<String> result = new ArrayList<String>();
			FileInputStream fis = new FileInputStream(path);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			String str = null;
			while ((str = br.readLine()) != null) {
				result.add(str);
			}
			fis.close();
			isr.close();
			br.close();
			return result;
		} catch (Exception e) {
			return null;
		}
	}

	public static double isSentiment(String word, ArrayList<String> positivewords, ArrayList<String> nagetivewords)
			throws Exception {
		if (word == null) {
			return 0;
		}
		double result = 0;
		for (int i = 0; i < positivewords.size(); i++) {
			if (positivewords.get(i).equals(word)) {
				result = 1;
				break;
			}
		}
		if (result == 0) {
			for (int i = 0; i < nagetivewords.size(); i++) {
				if (nagetivewords.get(i).equals(word)) {
					result = -1.5;
					break;
				}
			}
		}
		return result;
	}

	public static boolean isDegree(String word, ArrayList<String> Degreewords) throws Exception {
		boolean result = false;
		for (int i = 0; i < Degreewords.size(); i++) {
			if (Degreewords.get(i).equals(word)) {
				result = true;
				break;
			}
		}
		return result;
	}

	public static boolean isOpposite(String word, ArrayList<String> Oppositewords) throws Exception {
		boolean result = false;
		for (int i = 0; i < Oppositewords.size(); i++) {
			if (Oppositewords.get(i).equals(word)) {
				result = true;
				break;
			}
		}
		return result;

	}

	public static double sentimentScore(double sentiment, int wordDegree, int opposite) {
		double result = 0.0;
		// System.out.println(sentiment + " " + wordDegree + " " + opposite);
		result = sentiment * wordDegree * opposite;
		return result;
	}
}
