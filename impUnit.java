package group20;

import genius.core.issue.Value;
import java.util.Comparator;

public class impUnit {
    public Value valueOfIssue;
    public int weightSum = 0;
    public int count = 0;
    public double meanWeightSum = 0.0D;

    public impUnit(Value var1) {
        this.valueOfIssue = var1;
    }

    public String toString() {
        return String.format("%s %f", this.valueOfIssue, this.meanWeightSum);
    }

    static class meanWeightSumComparator implements Comparator<group20.impUnit> {
        meanWeightSumComparator() {
        }

        public int compare(group20.impUnit o1, group20.impUnit o2) {
            if (o1.meanWeightSum < o2.meanWeightSum) {
                return 1;
            } else {
                return o1.meanWeightSum > o2.meanWeightSum ? -1 : 0;
            }
        }
    }
}