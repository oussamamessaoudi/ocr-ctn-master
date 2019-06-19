package com.pca;

public class Score {
    private String valueDoc;
    private String valueOCR;
    private double score;

    public Score(String valueDoc,String valueOCR,double score){
        this.valueDoc = valueDoc;
        this.valueOCR = valueOCR;
        this.score = score;
    }

    public String getValueDoc() {
		return valueDoc;
	}



	public void setValueDoc(String valueDoc) {
		this.valueDoc = valueDoc;
	}



	public String getValueOCR() {
		return valueOCR;
	}



	public void setValueOCR(String valueOCR) {
		this.valueOCR = valueOCR;
	}

	public double getScore() {
        return score;
    }

	@Override
	public String toString() {
		return "Score [valueDoc=" + valueDoc + ", valueOCR=" + valueOCR
				+ ", score=" + score + "]";
	}

}