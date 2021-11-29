//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package group20;

import group20.impUnit.meanWeightSumComparator;
import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.uncertainty.UserModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class ImpMap extends HashMap<Issue, List<impUnit>> {
    public ImpMap(UserModel userModel) {
        Iterator iterator = userModel.getDomain().getIssues().iterator();

        while(iterator.hasNext()) {
            Issue issue = (Issue)iterator.next();
            IssueDiscrete issueDiscrete = (IssueDiscrete)issue;//discrete issue
            ArrayList ImpList = new ArrayList();
            int numberOfValues = issueDiscrete.getNumberOfValues();

            for(int i = 0; i < numberOfValues; ++i) {
                ImpList.add(new impUnit(issueDiscrete.getValue(i)));
            }

            this.put(issue, ImpList);
        }

    }

    public void opponent_update(Bid var1) {
        Iterator var2 = var1.getIssues().iterator();

        while(true) {
            while(var2.hasNext()) {
                Issue var3 = (Issue)var2.next();
                int var4 = var3.getNumber();
                List var5 = (List)this.get(var3);
                Iterator var6 = var5.iterator();

                while(var6.hasNext()) {
                    impUnit var7 = (impUnit)var6.next();
                    if (var7.valueOfIssue.toString().equals(var1.getValue(var4).toString())) {
                        ++var7.meanWeightSum;
                        break;
                    }
                }
            }

            var2 = this.values().iterator();

            while(var2.hasNext()) {
                List var8 = (List)var2.next();
                var8.sort(new meanWeightSumComparator());
            }

            return;
        }
    }

    public void self_update(UserModel userModel) {
        int var2 = 0;
        Iterator bidIterator = userModel.getBidRanking().getBidOrder().iterator();

        label86:
        while(bidIterator.hasNext()) {
            Bid next = (Bid)bidIterator.next();
            ++var2;
            List issueList = next.getIssues();
            Iterator iterator = issueList.iterator();

            while(true) {
                while(true) {
                    if (!iterator.hasNext()) {
                        continue label86;//when the end of issue list
                    }

                    Issue nextIssue = (Issue)iterator.next();
                    int nextIssueNumber = nextIssue.getNumber();
                    List var9 = (List)this.get(nextIssue);
                    Iterator var10 = var9.iterator();

                    while(var10.hasNext()) {
                        impUnit var11 = (impUnit)var10.next();
                        if (var11.valueOfIssue.toString().equals(next.getValue(nextIssueNumber).toString())) {
                            var11.weightSum += var2;
                            ++var11.count;
                            break;
                        }
                    }
                }
            }
        }

        bidIterator = this.values().iterator();

        List var13;
        Iterator var14;
        while(bidIterator.hasNext()) {
            var13 = (List)bidIterator.next();
            var14 = var13.iterator();

            while(var14.hasNext()) {
                impUnit var15 = (impUnit)var14.next();
                if (var15.count == 0) {
                    var15.meanWeightSum = 0.0D;
                } else {
                    var15.meanWeightSum = (double)var15.weightSum / (double)var15.count;
                }
            }
        }

        bidIterator = this.values().iterator();

        while(bidIterator.hasNext()) {
            var13 = (List)bidIterator.next();
            var13.sort(new meanWeightSumComparator());
        }

        double var12 = 1.0D / 0.0;
        var14 = this.entrySet().iterator();

        while(var14.hasNext()) {
            Entry var16 = (Entry)var14.next();
            double var18 = ((impUnit)((List)var16.getValue()).get(((List)var16.getValue()).size() - 1)).meanWeightSum;
            if (var18 < var12) {
                var12 = var18;
            }
        }

        var14 = this.values().iterator();

        while(var14.hasNext()) {
            List var17 = (List)var14.next();

            impUnit var20;
            for(Iterator var19 = var17.iterator(); var19.hasNext(); var20.meanWeightSum -= var12) {
                var20 = (impUnit)var19.next();
            }
        }

    }

    public double getImportance(Bid var1) {
        double var2 = 0.0D;

        double var7;
        for(Iterator var4 = var1.getIssues().iterator(); var4.hasNext(); var2 += var7) {
            Issue var5 = (Issue)var4.next();
            Value var6 = var1.getValue(var5.getNumber());
            var7 = 0.0D;
            Iterator var9 = ((List)this.get(var5)).iterator();

            while(var9.hasNext()) {
                impUnit var10 = (impUnit)var9.next();
                if (var10.valueOfIssue.equals(var6)) {
                    var7 = var10.meanWeightSum;
                    break;
                }
            }
        }

        return var2;
    }
}
