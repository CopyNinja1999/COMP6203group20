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

public class geneticAlgorithm {
    private UserModel userModel;
    private Random random = new Random(); //用于生成随机数

    private List<AbstractUtilitySpace> population = new ArrayList<AbstractUtilitySpace>();  //用于存放所有的累加效用空间population
    private int popSize = 500;         //每一个population的总数
    private int maxIterNum = 3;      //最大迭代的次数
    private double mutationRate = 0.04;//变异几率

    //构造函数。实例该类的同时，必须得传入UserModel，这个东西可以帮助我们获得当前domain下我们需要的各种信息⚽️。
    public geneticAlgorithm(UserModel userModel) {
        this.userModel = userModel;
    }
    //函数主体，返回一个预测的效用空间。
    public AbstractUtilitySpace geneticAlgorithm(){
        //初始化种群
        for(int i=0; i<popSize*4;i++){
            population.add(getRandomChromosome());  //此时种群里有2000个。后面会择优筛掉1500个
        }

        //重复迭代maxiterNum次 170
        for(int num=0;num<maxIterNum;num++){
            List<Double> fitnessList=new ArrayList<>();
//popu size=2000
            for(int i=0;i<population.size();i++){
                fitnessList.add(getFitness(population.get(i)));
            }

            //轮盘选择这些population。
            population=select(population,fitnessList,popSize);

            //crossover,crossover的时候考虑变异
            for(int i=0;i<popSize*0.1;i++){
                AdditiveUtilitySpace father=(AdditiveUtilitySpace) population.get(random.nextInt(popSize));
                AdditiveUtilitySpace mother=(AdditiveUtilitySpace) population.get(random.nextInt(popSize));
                AbstractUtilitySpace child=crossover(father,mother);
                population.add(child);
            }

        }

        //对最后一个种群只挑选最好的，作为最后的答案。。防止遇到突然变异，导致误差瞬间上升
        List<Double> lastFitnessList=new ArrayList<>();
        for(AbstractUtilitySpace i:population){
            lastFitnessList.add(getFitness(i));
        }
        double bestFitness=Collections.max(lastFitnessList);
        int index=lastFitnessList.indexOf(bestFitness);
        System.out.print("结果是:");
        getFitness(population.get(index));

        return  population.get(index);
    }
    private double getFitness(AbstractUtilitySpace abstractUtilitySpace){
        BidRanking bidRanking = userModel.getBidRanking();   //1.先从userModel中取出bidRanking列表

        //先把bidRanking存放在一个列表了。不然的话，待会不能靠索引去取值。
        List<Bid> bidRankingStore=new ArrayList<>();
        for(Bid bid:bidRanking){
            bidRankingStore.add(bid);
        }

        //2.我们要单独写一个bidList去存放bidRanking去防止计算量过大。
        List<Bid> bidList =new ArrayList<>();
        for(Bid bid:bidRanking) {bidList.add(bid);}
        //如果bid量小于400
//        if(bidRanking.getSize()<=400){
//            for(Bid bid:bidRanking){
//                bidList.add(bid);
//            }
//        }
//
//        //如果bid量在400和800之间
//        else if(bidRanking.getSize()>400&&bidRanking.getSize()<800){
//            for(int i=0;i<bidRanking.getSize();i+=2){
//                bidList.add(bidRankingStore.get(i));
//            }
//        }


        List<Double> utilityList=new ArrayList<>();
        for(Bid bid:bidList){
            utilityList.add(abstractUtilitySpace.getUtility(bid));   //计算在当前空间下，每个bidRanking的实际效用是多少。并且放入utilityList中。
        }                                                             //注意，此时的utilityList的索引和bidRanking的索引是相同的。我们需要利用这个存放在TreeMap中



        TreeMap<Integer,Double> utilityRank=new TreeMap<>();   //构建treeMap，一个存放一下当前的索引，一个存放对应索引的utility。

        for(int i=0;i<utilityList.size();i++){   //这里对utility进行遍历，将索引和效用存放在TreeMap中。
            utilityRank.put(i,utilityList.get(i));
        }

        //4. 此时我们需要根据TreeMap的值进行排序（值中存放的是效用值）
        Comparator<Map.Entry<Integer,Double>> valueComparator = Comparator.comparingDouble(Map.Entry::getValue);
        // map转换成list进行排序
        List<Map.Entry<Integer,Double>> listRank = new ArrayList<>(utilityRank.entrySet());
        // 排序
        Collections.sort(listRank, valueComparator);

        //用以上的方法，TreeMap此时就被转换成了List。这tm什么方法我也很烦躁。。
        //list现在长这个样子。[100=0.3328030236029489, 144=0.33843867914476017, 82=0.35366230775310603, 68=0.39994535024458255, 25=0.4407324473062739, 119=0.45895568095691974,
        //不过这也有个好处。就是列表的索引值，可以表示为utilityList的索引值。

        int error=0;
        for(int i=0;i<listRank.size();i++){
            int gap=Math.abs(listRank.get(i).getKey()-i);  //5. 这里的i其实可以对应utilityList的索引i。假设i=1.此时在utilityList中的效用应该是最低值。
            error+=gap*gap;
        }                                             //但是，在listRank中，效用最低的值对应的index竟然是100。那说明，这个效用空间在第一个位置差了很大。
        // 同理，如果listRank中的每一个键能正好接近或者等于它所在的索引数，那么说明这个效用空间分的就很对。

        //6. 对数思想，需要的迭代次数最少
        double score=0.0f;
        double x=error/(Math.pow(listRank.size(), 3));
        double theta=-15*Math.log(x+0.00001f);  //利用对数思想   -15
        score=theta;
        System.out.println("Error:"+error);  //7. 监控每次迭代的error的大小

        return score;  //8. 返回fitness score

    }
    //产生一个随机的效用空间
    private AbstractUtilitySpace getRandomChromosome(){
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());  //直接获得当前utilitySpace下的domain.
        List<Issue> issues=additiveUtilitySpaceFactory.getDomain().getIssues();
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
    //select算法，基于轮盘赌算法和精英策略。
    private List<AbstractUtilitySpace> select(List<AbstractUtilitySpace> population, List<Double> fitnessList, int popSize){
        int eliteNumber=2;   //保留多少个精英
        List<AbstractUtilitySpace> nextPopulation=new ArrayList<>();  //用来存放下一代


        //这一步我们需要先复制fitnessList。。
        List<Double> copyFitnessList=new ArrayList<>();
        for(int i=0;i<fitnessList.size();i++){
            copyFitnessList.add(fitnessList.get(i));
        }

        //复制的好处，就是对copyFitnessList的修改不会影响，fitnessList
        for(int i=0;i<eliteNumber;i++){
            double maxFitness=Collections.max(copyFitnessList);   //先选取最大的效用
            int index=copyFitnessList.indexOf(maxFitness);        //根据最大的效用，选取最大效用的index
            nextPopulation.add(population.get(index));        //将最大效用的效用空间存放在下一代

            double temp=-1000.0;  //初始化的值得很小。。因为在fitness函数中真的有机会得到很小的负值
            Double tempDouble=new Double(temp);
            //这里需要注意，每次找到一个精英，就需要把这个精英的效用降低为-1，以便让第二次循环找到第二个最大值
            copyFitnessList.set(index,tempDouble);
        }

        //先保存所有的fitness
        double sumFitness=0.0;
        for(int i=0;i<eliteNumber;i++){
            sumFitness+=fitnessList.get(i);
        }
//sum of elite fitness
        //轮盘赌算法
        for(int i=0;i<popSize-eliteNumber;i++){
            double randNum=random.nextDouble()*sumFitness;
            double sum=0.0;
            for(int j=0;i<population.size();j++){
                sum+=fitnessList.get(i);
                if(sum>randNum){
                    nextPopulation.add(population.get(j));
                    break;
                }
            }
        }

        return nextPopulation;
    }
    private AbstractUtilitySpace crossover(AdditiveUtilitySpace father,AdditiveUtilitySpace mother){
        double wFather;
        double wMother;
        double wUnion;
        double mutationStep=0.35;    //1. 变异最大步长 0.35


        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());
        List<IssueDiscrete> issuesList=additiveUtilitySpaceFactory.getIssues();
        for(IssueDiscrete i:issuesList){
            wFather=father.getWeight(i);  //获取父亲的权重
            wMother=mother.getWeight(i);  //获取母亲的权重

            //2. 这里判断基因是偏向父亲还是偏向于母亲
            wUnion=(wFather+wMother)/2;
            if (Math.random()>0.5){
                double wChild=wUnion+mutationStep*Math.abs(wFather-wMother);
                if (wChild < 0.01) wChild = 0.01;  //权重的最小单位就是0.01
                additiveUtilitySpaceFactory.setWeight(i,wChild);
            }
            else {
                double wChild=wUnion-mutationStep*(Math.abs(wFather-wMother));
                if (wChild < 0.01) wChild = 0.01;
                additiveUtilitySpaceFactory.setWeight(i,wChild);
            }
            //3. 考虑变异情况
            if(random.nextDouble()<mutationRate) additiveUtilitySpaceFactory.setWeight(i,random.nextDouble());

            //4. 每个issue下的value也是有自己的权重的
            for(ValueDiscrete v:i.getValues()){
                wFather=((EvaluatorDiscrete)father.getEvaluator(i)).getDoubleValue(v);
                wMother=((EvaluatorDiscrete)mother.getEvaluator(i)).getDoubleValue(v);
                //这里判断哪个父母的基因最好了
                wUnion=(wFather+wMother)/2;

                if (Math.random()>0.5){
                    double wChild=wUnion+mutationStep*Math.abs(wFather-wMother);
                    if (wChild < 0.01) wChild = 0.01;
                    additiveUtilitySpaceFactory.setUtility(i,v,wChild);
                }
                else {
                    double wChild = wUnion - mutationStep * Math.abs(wFather - wMother);
                    if (wChild < 0.01) wChild = 0.01;
                    additiveUtilitySpaceFactory.setUtility(i, v, wChild);
                }
                //考虑变异情况
                if(random.nextDouble()<mutationRate) additiveUtilitySpaceFactory.setUtility(i,v,random.nextDouble());
            }
        }
        additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }
}