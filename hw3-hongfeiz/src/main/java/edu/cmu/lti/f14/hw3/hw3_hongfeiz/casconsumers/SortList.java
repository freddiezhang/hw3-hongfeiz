package edu.cmu.lti.f14.hw3.hw3_hongfeiz.casconsumers;

/**
 * 
 * @author freddie
 * rewrite compareTo method to define a comparison rule in SortList class
 *
 */
public class SortList implements Comparable<SortList>{
  
    int Id;
    int rel;
    int sId;
    double Similarity;
    SortList(int sId, int Id, int rel, double Similarity) {
    this.sId = sId;
    this.Id = Id;
    this.rel = rel;
    this.Similarity = Similarity;
    }
    public double getSim() {
      return Similarity;
    }
    public int getSId() {
      return sId;
    }
    @Override
    public int compareTo(SortList o) {
      if (this.Similarity - o.Similarity < 0)
        return 1;
      else if (this.Similarity - o.Similarity > 0)
        return -1;
      else
        return this.rel == 1 ? 0 : 1;
    }
 
}
