package group20;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.*;

public class SimulatedAnnealingModel {
    private UserModel userModel;
    private Random random = new Random(); //for random numbers
    private double Temp=1e5;//a temperature controlling probabilities of downward steps
    private double minTemp=1e-3;
    private double alpha=0.96;//annealing rate
    private final double lowUtility;
    private final double highUtility;
    private final int MAXCOUNT=100000;
    public SimulatedAnnealingModel(UserModel userModel){
        this.userModel=userModel;
        BidRanking bidRanking=userModel.getBidRanking();
        lowUtility=bidRanking.getLowUtility();
        highUtility=bidRanking.getHighUtility();
    }
    public AbstractUtilitySpace simulatedAnnealingAlgorithm(){
        //initializing
        BidRanking bidRanking= userModel.getBidRanking();
        AbstractUtilitySpace oldUtilitySpace=getRandomSpace();
        AbstractUtilitySpace newUtilitySpace=oldUtilitySpace;
        int counter=0;
        while(Temp>minTemp){
         //random bias here
         newUtilitySpace=disturbingNewSpace(oldUtilitySpace);
        double oldEval=evaluateModel(oldUtilitySpace);
        double newEval=evaluateModel(newUtilitySpace);
        double dE=newEval-oldEval;
        if(judge(dE,Temp))//new utilitySpace accepted
        {oldUtilitySpace=newUtilitySpace;


        }
        else if(dE<0)
        Temp=Temp*alpha;
        else counter+=1;
        if(counter>MAXCOUNT){break;}
        }




        return oldUtilitySpace;
    }
    /*
    Used to evaluate random models. The more similarity to the given bidranking the better
     */
    private double evaluateModel(AbstractUtilitySpace abstractUtilitySpace){
        BidRanking bidRanking = userModel.getBidRanking();

        //??????bidRanking???????????????????????????????????????????????????????????????????????????
        List<Bid> bidRankingStore=new ArrayList<>();
        for(Bid bid:bidRanking){
            bidRankingStore.add(bid);
        }
        List<Bid> bidList =new ArrayList<>();
        for(Bid bid:bidRanking) {bidList.add(bid);}
        List<Double> utilityList=new ArrayList<>();
        for(Bid bid:bidList){
            utilityList.add(abstractUtilitySpace.getUtility(bid));   //?????????????????????????????????bidRanking???????????????????????????????????????utilityList??????
        }                                                             //??????????????????utilityList????????????bidRanking?????????????????????????????????????????????????????????TreeMap???
        TreeMap<Integer,Double> utilityRank=new TreeMap<>();   //??????treeMap??????????????????????????????????????????????????????????????????utility???
        for(int i=0;i<utilityList.size();i++){   //?????????utility??????????????????????????????????????????TreeMap??????
            utilityRank.put(i,utilityList.get(i));
        }
        Comparator<Map.Entry<Integer,Double>> valueComparator = Comparator.comparingDouble(Map.Entry::getValue);
        List<Map.Entry<Integer,Double>> listRank = new ArrayList<>(utilityRank.entrySet());
        Collections.sort(listRank, valueComparator);
        int error=0;
        for(int i=0;i<listRank.size();i++){
            int gap=Math.abs(listRank.get(i).getKey()-i);
            error+=gap*gap;
        }
        double score=0.0f;
        double x=error/(Math.pow(listRank.size(), 3));
        double theta=-15*Math.log(x+0.00001f);  //??????????????????   -15
        score=theta;
        System.out.println("Error:"+error);  //7. ?????????????????????error?????????

        return score;  //8. ??????fitness score

    }
    /*
    Higher temperate tolerates a slightly worse solution, in order to get a globle maximum
     */
    private boolean judge(double dE, double temp){
        if(dE>0){return true;}
        else{
            double prob=Math.exp(-dE/temp);
            if(prob>random.nextDouble()) return true;                //accepted
            else return false;                                      //Else rejected
        }
    }
    private AbstractUtilitySpace disturbingNewSpace(AbstractUtilitySpace oldUtilitySpace){
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());
        AdditiveUtilitySpace oldAdditiveUtilitySpace=(AdditiveUtilitySpace)oldUtilitySpace;
        List<Issue> issues=additiveUtilitySpaceFactory.getDomain().getIssues();
        for(Issue issue:issues){
            double oldweight=oldAdditiveUtilitySpace.getWeight(issue);
            double newWeight=0;
            newWeight=oldweight+0.5*(random.nextGaussian()-0.5);//[oldWeight,oldWeight+1], mean=oldWeight}
           if(newWeight<0.01) newWeight=0.01;

            additiveUtilitySpaceFactory.setWeight(issue,newWeight);    //set weight for every issue
            IssueDiscrete values=(IssueDiscrete) issue;
            EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) oldAdditiveUtilitySpace.getEvaluator(issue.getNumber());
            for (ValueDiscrete valueDiscrete:values.getValues()){
                double oldEvaluation=0;
                try{  oldEvaluation=evaluatorDiscrete.getEvaluation(valueDiscrete);}
              catch(Exception e){e.printStackTrace();}

                double newEvaluation=0;
                newEvaluation=oldEvaluation+0.5*(random.nextGaussian()-0.5);
                if(newEvaluation<0.01) newEvaluation=0.01;
                additiveUtilitySpaceFactory.setUtility(issue,valueDiscrete,newEvaluation);
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }
    /*
    return a random utilitySpace
     */

    private AbstractUtilitySpace getRandomSpace(){
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());//build empty UtilityFactory by domain
        List<Issue> issues=additiveUtilitySpaceFactory.getDomain().getIssues();
        for(Issue issue:issues){
            additiveUtilitySpaceFactory.setWeight(issue,random.nextDouble());    //set weight for every issue
            IssueDiscrete values=(IssueDiscrete) issue;
            for (Value value:values.getValues()){
                additiveUtilitySpaceFactory.setUtility(issue,(ValueDiscrete)value,random.nextDouble());
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights(); //since the evaluations are random, normalization is needed.
        return  additiveUtilitySpaceFactory.getUtilitySpace();

    }
}
