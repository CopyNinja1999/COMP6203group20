package group20;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Iterator;
import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
// Genetic based algorithm, inspired by:
// Watson, R.A., 2004, September. A simple two-module problem to exemplify building-block assembly under crossover.
// In International Conference on Parallel Problem Solving from Nature (pp. 161-171). Springer, Berlin, Heidelberg.

public class GeneticEstimatesUtility {

	
	private UserModel userModel;
    private Random random=new Random(); //for generating random numbers

    private ArrayList<AbstractUtilitySpace> population=new ArrayList<AbstractUtilitySpace>();
    /*
    Hyperparameters here
     */
    private final int popSize=500;
    private final int initPop=popSize*4;
    private final double choseParents=popSize*0.1;
    private final int maxIterNum=200;
    private final double mutationRate=0.04;
    private final int numElite=2;
    private final double mutationStep=0.35;

    public GeneticEstimatesUtility(UserModel userModel) {
    	this.userModel = userModel;
    }
    /*
    Main body of the genetic algorithm
     */
    public AbstractUtilitySpace geneticUtilitySpace() 
    {
     	
    	for(int i= 0;i<initPop;i++)
    	{
            AbstractUtilitySpace  randomSpace = RandomChromosomeSpace();
    		population.add(randomSpace);
    		
    	}
    	
         for(int num=0;num<maxIterNum;num++)
         {
             List<Double> fitnessList=new ArrayList<>();
             for(int i=0;i<population.size();i++){
                 fitnessList.add(evaluateFitness(population.get(i)));
             }
            //roulette wheel select here, returns chosen spaces
             population = rouletteWheelSelect(population,fitnessList);

             for(int i = 0;i<choseParents;i++)
             {
                 AdditiveUtilitySpace father=(AdditiveUtilitySpace) population.get(random.nextInt(popSize));
                 AdditiveUtilitySpace mother=(AdditiveUtilitySpace) population.get(random.nextInt(popSize));
                 AbstractUtilitySpace child=crossover(father,mother);
                 population.add(child);

             }
         }
         List<Double> lastFitnessList=new ArrayList<>();
         for(AbstractUtilitySpace i:population){
             lastFitnessList.add(evaluateFitness(i));
         }
         double bestFitness=Collections.max(lastFitnessList);
         int index=lastFitnessList.indexOf(bestFitness);
         evaluateFitness(population.get(index));
         return  population.get(index);
    }
    /*
    The idea is evaluate the
     */
    
    private double evaluateFitness(AbstractUtilitySpace abstractUtilitySpace)
    {
    	BidRanking bidRanking = userModel.getBidRanking();
		List<Bid> bidOrder = bidRanking.getBidOrder();  //
		HashMap <Bid,Double>OldorderIndexHashMap = new HashMap<>();
		
		Double index = 1.0;
		for (Bid bid:bidOrder) 
		{
			OldorderIndexHashMap.put(bid,index);
			index++;
		}

		 TreeMap<Double, Bid> newUtilityTreeMap = new TreeMap<>();
		 
		 //tree structure: rank, <utility, bid>

		 for (Bid bid:bidOrder) {
			 double newUtility = abstractUtilitySpace.getUtility(bid);
			 if(!newUtilityTreeMap.containsKey(newUtility)) {newUtilityTreeMap.put(newUtility,bid); }
			 else {newUtilityTreeMap.put(newUtility+0.0000001,bid);}   //in case two space have the same utility

		 }

		 HashMap <Bid,Double>newOrderIndexHashMap =  new HashMap<>();

        //sort the new utility
		 Iterator<Double> it = newUtilityTreeMap.keySet().iterator();
		 double index2 = 1.0;
	        while(it.hasNext()){
	            Double key = it.next();
	            newOrderIndexHashMap.put(newUtilityTreeMap.get(key), index2);
	            index2++;
	        }
		 
	     double error = 0;
	     
	     for(int i = 0;i<bidOrder.size();i++) 
	     {
	    	Bid bid =  bidOrder.get(i);
	    	try {
	    	double absGap =  Math.abs(OldorderIndexHashMap.get(bid)-newOrderIndexHashMap.get(bid));
	    	error += Math.pow(absGap,2);
	    	}
	    	catch (NullPointerException e) {
                e.printStackTrace();
	    		continue;
			}
	    	
	     }
         //the less the error, the better the model
//		 double score = 0.0;
//		 double num = error/(Math.pow(bidOrder.size(), 3));
//		 System.out.println("BigggError:"+error);
//
//		  score = -15*Math.log(num);
        double score =1/error;
		  return score;
    }
    
    
    //code from Robin Luo and Peihao to generate random utility space.
    private AbstractUtilitySpace RandomChromosomeSpace()
    {
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<Issue> issues=additiveUtilitySpaceFactory.getDomain().getIssues();
        for(Issue issue:issues){
            additiveUtilitySpaceFactory.setWeight(issue,random.nextDouble());
            IssueDiscrete values=(IssueDiscrete) issue;
            for (Value value:values.getValues()){
                additiveUtilitySpaceFactory.setUtility(issue,(ValueDiscrete)value,random.nextDouble());
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return  additiveUtilitySpaceFactory.getUtilitySpace();

    }
    //randomly select from population, probability based on their fitness
    private ArrayList<AbstractUtilitySpace> rouletteWheelSelect(List<AbstractUtilitySpace> fatherPopulation, List<Double> fitnessList){
        double eliteScore = 0.0;
        double probability [] = new double[fitnessList.size()];
        double cumulative [] = new double [fitnessList.size()]; 
        ArrayList<AbstractUtilitySpace> nextGeneration = new ArrayList<>();//for storing next generation
        List<Double> tempFitnessList = new ArrayList<>();
        for(int i = 0;i<fitnessList.size();i++) 
        {
        	tempFitnessList.add(fitnessList.get(i));
        }
        //find elites whose utilities are the most
        for(int i = 0;i<numElite;i++)
        {   
        	int index = tempFitnessList.indexOf(Collections.max(tempFitnessList));
        	nextGeneration.add(fatherPopulation.get(index));
        	eliteScore += fitnessList.get(i);
        	
        	tempFitnessList.set(index, Double.valueOf(-100));
        }

        for(int i = 0; i<fitnessList.size();i++)
        {
        	probability[i] = fitnessList.get(i);
        	if(i==0) {cumulative[i] = probability[i];}
        	else {cumulative[i] = cumulative[i-1]+probability[i];}
        }

        //roulette wheel select
        for(int i = 0; i<popSize-numElite;i++)
        {
        	double randomNum = random.nextDouble()*eliteScore;//[0,eliteScore]
        	
        	for(int j=0;i<fatherPopulation.size();j++){
        		if(randomNum<cumulative[j]) 
        		{
        			nextGeneration.add(fatherPopulation.get(j));
        			break;
        		}
        	}

        }
//        System.out.println("Son population "+nextGeneration.size());
		return nextGeneration;

        
    }
    
 //One parents, one child
    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father,AdditiveUtilitySpace mother){
        double wFather;
        double wMother;
        double wUnion;
        AdditiveUtilitySpaceFactory childAdditiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<IssueDiscrete> issuesList=childAdditiveUtilitySpaceFactory.getIssues();
        //Update every issue weight, and every evaluation
        for(IssueDiscrete i:issuesList)
        {
            wFather=father.getWeight(i);
            wMother=mother.getWeight(i);
            wUnion=(wFather+wMother)/2;
            if (Math.random()>0.5){
                double wChild=wUnion+updateWeight(wFather,wMother);
                childAdditiveUtilitySpaceFactory.setWeight(i,wChild);
            }
            else {
                double wChild=wUnion-mutationStep*updateWeight(wFather,wMother);
                if (wChild < 0.01) wChild = 0.01;//in case the child weight goes too low
                childAdditiveUtilitySpaceFactory.setWeight(i,wChild);
            }
            //Here is the key point to the global optimal: mutation
            if(random.nextDouble()<mutationRate) childAdditiveUtilitySpaceFactory.setWeight(i,random.nextDouble());

            for(ValueDiscrete v:i.getValues()){
                wFather=((EvaluatorDiscrete)father.getEvaluator(i)).getDoubleValue(v);
                wMother=((EvaluatorDiscrete)mother.getEvaluator(i)).getDoubleValue(v);
                wUnion=(wFather+wMother)/2;

                if (Math.random()>0.5){
                    double wChild=wUnion+updateWeight(wFather,wMother);
                    childAdditiveUtilitySpaceFactory.setUtility(i,v,wChild);
                }
                else {
                    double wChild = wUnion - updateWeight(wFather,wMother);
                    if (wChild < 0.01) wChild = 0.01;
                    childAdditiveUtilitySpaceFactory.setUtility(i, v, wChild);
                }
                //mutation as well
                if(random.nextDouble()<mutationRate) childAdditiveUtilitySpaceFactory.setUtility(i,v,random.nextDouble());
            }
        }
        childAdditiveUtilitySpaceFactory.normalizeWeights();
        return childAdditiveUtilitySpaceFactory.getUtilitySpace();
    }
    public double updateWeight(double fatherWeight, double motherWeight)
    {
        return mutationStep*Math.abs(fatherWeight-motherWeight);

    }


    
   
}
