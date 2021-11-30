package group20;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Vector;

public class LinearRegressionModel {
    private UserModel userModel;
    private Random random = new Random(); //用于生成随机数
    private double MAXITER=1000;
    private Domain Domain= userModel.getDomain();
    private BidRanking bidRanking= userModel.getBidRanking();

    //构造函数。实例该类的同时，必须得传入UserModel，这个东西可以帮助我们获得当前domain下我们需要的各种信息⚽️。
    public LinearRegressionModel(UserModel userModel) {
        this.userModel = userModel;
    }
//    public AbstractUtilitySpace LRalgorithm(){
////        AbstractUtilitySpace RandomSpace=getRandomChromosome();
//
////        return RandomSpace;
//    }
    public double ErrorFunction(AbstractUtilitySpace UtilitySpace){
        double score=0.0D;
        int NumRanking=bidRanking.getSize();
        List<Bid> bidRankingStore=new ArrayList<>();
        for(Bid bid:bidRanking){
            bidRankingStore.add(bid);
        }
        List<Double>Target=new ArrayList<>();
        for(Bid bid:bidRanking){

            double rankScore=NumRanking-bidRanking.indexOf(bid);
            Target.add(rankScore);
        }
        HashMap<Integer,Vector<Integer>> InputSpace=new HashMap<>();
        for(Bid bid:bidRanking){
        List<Issue> issues=bid.getIssues();
        for(Issue issue:issues){
//            values=
        }

        }
        return score;
    }
    private AbstractUtilitySpace evaluationInitialize(){
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(Domain);  //直接获得当前utilitySpace下的domain.
        additiveUtilitySpaceFactory.estimateUsingBidRanks(bidRanking);//initialize evaluation
        List<Issue> issues=Domain.getIssues();
        for(Issue issue:issues){
            additiveUtilitySpaceFactory.setWeight(issue,random.nextDouble());    //设置每个issue的权重
            IssueDiscrete values=(IssueDiscrete) issue;       //将issue强制转换为values集合
            for (Value value:values.getValues()){            //通过values集合，获取每个value。
                additiveUtilitySpaceFactory.setUtility(issue,(ValueDiscrete)value,random.nextDouble());   //因为现在是累加效用空间，随便设置一个权重之后，可以对当前这个value设置一个效用，效用随机。
            }                                                                                            //当效用确定了之后，当前的value自己本身的值也就确定了。
            //这里设置的效用是设置value的evaluation
        }
        additiveUtilitySpaceFactory.normalizeWeights(); //因为之前对每个value的效用值计算都是随机的，这个时候，需要归一化。
        return  additiveUtilitySpaceFactory.getUtilitySpace();  //生成一个效用空间之后，返回这个效用空间。

    }
}
