package group20;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import agents.org.apache.commons.lang.math.DoubleRange;

import java.util.Random;
import java.util.TreeMap;

import javax.jws.soap.SOAPBinding;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;
import javax.swing.text.StyledEditorKit.ForegroundAction;

import java.util.Iterator;

import genius.core.Bid;

import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;

import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;
//import javafx.scene.shape.CullFace;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;

//这里复制的别人的 先这么写着
// Genetic algorithm ispired by:
// Watson, R.A., 2004, September. A simple two-module problem to exemplify building-block assembly under crossover.
// In International Conference on Parallel Problem Solving from Nature (pp. 161-171). Springer, Berlin, Heidelberg.

public class GeneticEstimatesUtility {

	
	private UserModel userModel;
    private Random random=new Random(); //用于生成随机数

    private ArrayList<AbstractUtilitySpace> population=new ArrayList<AbstractUtilitySpace>();  //用于存放所有的累加效用空间population
    private int popSize=50;         //每一个population的总数
    private int maxIterNum=10;      //最大迭代的次数
    private double mutationRate=0.04;//变异几率

    
    public GeneticEstimatesUtility(UserModel userModel) {
    	this.userModel = userModel;
    }
    
    //该方法 全部都是粘贴的。。。。
    public AbstractUtilitySpace geneticUtilitySpace() 
    {
    	for(int i= 0;i<popSize*2;i++)
    	{
    		population.add(getRandomUtilitySpace());
    		
    	}
    	
    	 for(int num=0;num<maxIterNum;num++){
             List<Double> fitnessList=new ArrayList<>();

             for(int i=0;i<population.size();i++){
                 fitnessList.add(fitness(population.get(i)));
             }
             
        	 population = selectElite(popSize,population,fitnessList);
        	 
        	 for(int i = 0;i<popSize*0.1;i++) 
        	 {
                 AdditiveUtilitySpace father=(AdditiveUtilitySpace) population.get(random.nextInt(popSize));
                 AdditiveUtilitySpace mother=(AdditiveUtilitySpace) population.get(random.nextInt(popSize));
                 AbstractUtilitySpace child=crossover(father,mother);
                 population.add(child);
        		 
        	 }
    	 }
    	 
         //对最后一个种群只挑选最好的，作为最后的答案。。防止遇到突然变异，导致误差瞬间上升
         List<Double> lastFitnessList=new ArrayList<>();
         for(AbstractUtilitySpace i:population){
             lastFitnessList.add(fitness(i));
         }
         double bestFitness=Collections.max(lastFitnessList);
         int index=lastFitnessList.indexOf(bestFitness);
         System.out.print("结果是:");
         fitness(population.get(index));

         return  population.get(index);
    	 
    	
    }
    
    
    
    private double fitness(AbstractUtilitySpace abstractUtilitySpace) 
    {
    	BidRanking bidRanking = userModel.getBidRanking();
		List<Bid> bidOrder = bidRanking.getBidOrder();  //
		HashMap <Bid,Double>OldorderIndexHashMap = new HashMap<>();
		
		Double index = 1.0; //存放排名和对应的bid，从小到大
		for (Bid bid:bidOrder) 
		{
			OldorderIndexHashMap.put(bid,index);
			
			index++;
		}
		System.out.println("OldOrder"+OldorderIndexHashMap.values());
		//List <Double> newUtilityList = new ArrayList<>();
		//for(Bid bid:bidOrder) {newUtilityList.add(abstractUtilitySpace.getUtility(bid));}
		
		 TreeMap<Double, Bid> newUtilityTreeMap = new TreeMap<>();
		 
		 //int i = 0;
		 for (Bid bid:bidOrder) {
			 double newUtility = abstractUtilitySpace.getUtility(bid);  //对应bid的新utility
			 newUtilityTreeMap.put(newUtility,bid);   //存入tree
			 //System.out.println(newUtilityTreeMap);
			 //i++;
		 }
		 
		 HashMap <Bid,Double>newOrderIndexHashMap =  new HashMap<>();
		
		 //这个代码网上找的 将tree总的排好序的key按顺序输出
		 Iterator<Double> it = newUtilityTreeMap.keySet().iterator();
		 double index2 = 1.0;
	        while(it.hasNext()){
	            Double key = it.next();
	            newOrderIndexHashMap.put(newUtilityTreeMap.get(key), index2); //将新排列的值输入新的Map
	            //System.out.println(key+"="+newUtilityTreeMap.get(key));
	            index2++;
	        }
		 
	     double error = 0;
	     System.out.println("Neworder"+newOrderIndexHashMap.values());
	     for(int i = 0;i<bidOrder.size();i++) 
	     {try{   	Bid bid =  bidOrder.get(i);
			 double absGap =  Math.abs(OldorderIndexHashMap.get(bid)-newOrderIndexHashMap.get(bid));//nullpointerE
			 error += absGap*absGap;}
		 catch(NullPointerException e){
			 System.err.println("exception caught");
			 continue;
		 }

	     }
	     
	     //这个地方需要自己调一下参数
		 double score = 0.0;
		 double num = error/(Math.pow(bidOrder.size(), 3));
		 return score = -15*Math.log(num); //参考的代码用的是15 目前是error越小分数越高
    }
    //code from Robin Luo and Peihao to generate random utility space.
    // 这个代码是整个引用的 标记一下吧 注释也是原来的
    private AbstractUtilitySpace getRandomUtilitySpace()
    {
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain()); 
        List<Issue> issues=additiveUtilitySpaceFactory.getDomain().getIssues();
        
        for(Issue issue:issues) 
        {	
        	additiveUtilitySpaceFactory.setWeight(issue,random.nextDouble());    //设置每个issue的权重
        	IssueDiscrete values=(IssueDiscrete) issue;       //将issue强制转换为values集合
            for (Value value:values.getValues()){            //通过values集合，获取每个value。
                additiveUtilitySpaceFactory.setUtility(issue,(ValueDiscrete)value,random.nextDouble());   
                //因为现在是累加效用空间，随便设置一个权重之后，可以对当前这个value设置一个效用，效用随机。
            } 
        }
        additiveUtilitySpaceFactory.normalizeWeights(); //因为之前对每个value的效用值计算都是随机的，这个时候，需要归一化。
        return  additiveUtilitySpaceFactory.getUtilitySpace();

    }
    
    private ArrayList<AbstractUtilitySpace> selectElite( int populationSize,List<AbstractUtilitySpace> fatherPopulation, List<Double> fitnessList){
        int remainElite=2;   //保留多少个精英
        double totalScore = 0.0;
        double probability [] = new double[fitnessList.size()];
        double cumulative [] = new double [fitnessList.size()]; 
        ArrayList<AbstractUtilitySpace> sonPopulation = new ArrayList<>();
        
        for(int i=0;i<fitnessList.size();i++) 
        {
        	totalScore+=fitnessList.get(i);
        }
        
        for(int i = 0;i<remainElite;i++)
        {   
        	int index = fitnessList.indexOf(Collections.max(fitnessList));
        	sonPopulation.add(fatherPopulation.get(index));
        	//fitnessList.remove(index);//将选择过的移除，移除后 index会改变吗
        	System.out.println("index of "+index+" will be elite and the remain amount is "+fitnessList.size());
        	
        }
        
        for(int i = 0; i<fitnessList.size();i++)
        {
        	probability[i] = fitnessList.get(i)/totalScore;
        	if(i==0) {cumulative[i] = probability[i];}
        	else {cumulative[i] = cumulative[i-1]+probability[i];}
        }
        
        for(int i = 0; i<fitnessList.size();i++) 
        {
        	double random = Math.random(); //这个地方估计选的会有问题
        	if(random>=cumulative[0]) {sonPopulation.add(fatherPopulation.get(i));}
        	else {
        		for(int j = 1;j<fitnessList.size();j++)
        		{
        			if(random>=cumulative[j]) 
        			{
        				sonPopulation.add(fatherPopulation.get(i));
        				break;
        			}
        		}
        	}
        }
        System.out.println("Son population "+sonPopulation.size());
        //轮盘
        
//        double slice = Math.random() * totalScore;
//		
//		double averageScore = totalScore / populationSize;
//		//因为精度问题导致的平均值大于最好值，将平均值设置成最好值
//		//averageScore = averageScore > bestScore ? bestScore : averageScore;
//		
//		
//		for (int i=0;i<fitnessList.size();i++) {  //这里popsize 我觉得应该是没用的 scoreList 已经删除了一些数了
//			
//			if (sum > slice && fitnessList.get(i) >= averageScore) {
//				sonPopulation.add(fatherPopulation.get(i)); //这个地方你也不知道 还剩下多少用来交配的
//				System.out.println("Son### population "+sonPopulation.size());
//			}
//		}
		
		//System.out.println("Son population "+sonPopulation.size());
		return sonPopulation;

        
    }
    
    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father,AdditiveUtilitySpace mather) 
    {
    	double fatherWeight;
        double matherWeight;
        
        double sonWeight;
        double mutStep=0.35;   //变异参数

        
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());
        //List<IssueDiscrete> issues=additiveUtilitySpaceFactory.getIssues();
		List< Issue > issues = additiveUtilitySpaceFactory.getDomain().getIssues();

        for(Issue issue:issues)  //这里或许会有问题
        {
        	fatherWeight = father.getWeight(issue);
        	matherWeight = mather.getWeight(issue);
        	
        	
        	
        	if(Math.random()>0.5) 
        	{
        		sonWeight = addWeight(true,fatherWeight, matherWeight,mutStep);
        		if(sonWeight < 0.001) {sonWeight=0.001;}
        		additiveUtilitySpaceFactory.setWeight(issue,sonWeight);
        	}
        	else if(Math.random()<=0.5)
        	{
        		sonWeight = addWeight(false,fatherWeight, matherWeight,mutStep);
        		if(sonWeight < 0.001) {sonWeight=0.001;}
        		additiveUtilitySpaceFactory.setWeight(issue,sonWeight);
        	}
        	else if(random.nextDouble()<mutationRate) 
        	{
        		additiveUtilitySpaceFactory.setWeight(issue,random.nextDouble()+0.001);
        		//随机值大于0。01
        		
        	}
        	//这块也不知道对不对 先这么写
        	IssueDiscrete issueDiscrete = (IssueDiscrete)issue;
        	//issue 的value权重
        	for(ValueDiscrete value:issueDiscrete.getValues()) 
        	{
        		fatherWeight = ((EvaluatorDiscrete)father.getEvaluator(issueDiscrete)).getDoubleValue(value);
            	matherWeight = ((EvaluatorDiscrete)mather.getEvaluator(issueDiscrete)).getDoubleValue(value);
            	
            	if(Math.random()>0.5) 
            	{
            		sonWeight = addWeight(true,fatherWeight, matherWeight,mutStep);
            		if(sonWeight < 0.001) {sonWeight=0.001;}
            		additiveUtilitySpaceFactory.setUtility(issueDiscrete, value, sonWeight);
            	}
            	else if(Math.random()<=0.5)
            	{
            		sonWeight = addWeight(false,fatherWeight, matherWeight,mutStep);
            		if(sonWeight < 0.001) {sonWeight=0.001;}
            		additiveUtilitySpaceFactory.setUtility(issueDiscrete, value, sonWeight);
            	}
            	else if(random.nextDouble()<mutationRate) 
            	{
            		additiveUtilitySpaceFactory.setUtility(issueDiscrete, value, random.nextDouble()+0.001);
            		//随机值大于0。01
            		
            	}
        	}	       	
        }
    	//原文复制粘贴
    	additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();

    }
    
    public double addWeight(Boolean who,double weight1,double weight2,double mutStep) 
    {	
    	double weight3 = (weight1+weight2)/2;
    	if(who)
    	{
    		return weight3+mutStep*Math.abs(weight1-weight2);
    	}
    	else
    	{
    		return weight3-mutStep*Math.abs(weight1-weight2);
    	}
    	
    }
    
   
}
