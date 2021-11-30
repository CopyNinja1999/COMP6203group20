package group20;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.misc.ScoreKeeper;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.UserModel;
import genius.core.utility.AbstractUtilitySpace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
public class LinearRegressionModel {
    private UserModel userModel;
//    private Random random = new Random(); 
//    private double MAXITER=1000;
//    private Domain Domain= userModel.getDomain();
//    private BidRanking bidRanking= userModel.getBidRanking();
    public LinearRegressionModel(UserModel userModel) {
        this.userModel = userModel;
    }
    public AbstractUtilitySpace LRalgorithm(){
        AbstractUtilitySpace utilitySpace=pseudoInverse();

        return utilitySpace;
    }
    public double ErrorFunction(AbstractUtilitySpace UtilitySpace){
    	AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());
        double score=0.0D;
        BidRanking bidRanking =userModel.getBidRanking();
        int NumRanking=bidRanking.getSize();
        List<Bid> bidRankingStore=new ArrayList<>();
        for(Bid bid:bidRanking){
            bidRankingStore.add(bid);
        }
        List<Bid> bidList =new ArrayList<>();
        for(Bid bid:bidRanking) {bidList.add(bid);}
        List<Double> utilityList=new ArrayList<>();
        for(Bid bid:bidList){
            utilityList.add(UtilitySpace.getUtility(bid));   //计算在当前空间下，每个bidRanking的实际效用是多少。并且放入utilityList中。
        }                                                             //注意，此时的utilityList的索引和bidRanking的索引是相同的。我们需要利用这个存放在TreeMap中
        
        return score;
    }
    public double getFitness(AbstractUtilitySpace abstractUtilitySpace){
        //先把bidRanking存放在一个列表了。不然的话，待会不能靠索引去取值。
    	BidRanking bidRanking =userModel.getBidRanking();
        List<Bid> bidRankingStore=new ArrayList<>();
        for(Bid bid:bidRanking){
            bidRankingStore.add(bid);
        }
        //2.我们要单独写一个bidList去存放bidRanking去防止计算量过大。
        List<Bid> bidList =new ArrayList<>();
        
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

for(Bid bid:bidRanking) {bidList.add(bid);}
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
	public static RealMatrix getPseudoInverse(RealMatrix x) {
		RealMatrix tempMatrix=x.transpose().multiply(x);
		RealMatrix pMatrix=new SingularValueDecomposition(tempMatrix).getSolver().getInverse().multiply(x.transpose());
		
//		System.out.print(pMatrix);
		return pMatrix;
	}
    private AbstractUtilitySpace pseudoInverse() {
    	AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=evaluationInitialize();
    	BidRanking bidRanking =userModel.getBidRanking();
        int NumRanking=bidRanking.getSize();
        int NumIssues=additiveUtilitySpaceFactory.getIssues().size();
        double[][]inputMatrix=new double[NumRanking][NumIssues];
        double[][] target=new double[NumRanking][1];
        //obtain input space and target matrix
        List<Bid> bidRankingStore=new ArrayList<>();
        for(Bid bid:bidRanking){
            bidRankingStore.add(bid);
        }
//        List<Double>Target=new ArrayList<>();
        int k=0;
        for(Bid bid:bidRanking){
            double rankScore=NumRanking-bidRanking.indexOf(bid);
//            System.out.println("rank="+bidRanking.indexOf(bid)+"Score="+rankScore);
            target[k][0]=rankScore;
//            System.out.println("Imported"+target[k][0]);
            k++;
        }
        HashMap<Integer,Vector<Double>> InputSpace=new HashMap<>();
              for(int j=0;j<NumRanking;j++) {
//        	System.out.println("target="+target[j][0]);
        }        	
              int i=0;
        for(Bid bid:bidRanking){

        List<Issue> issues=bid.getIssues();
        Vector<Double> evals=new Vector<Double>(2);
        int j=0;
        for(Issue issue:issues){
        	
            IssueDiscrete values=(IssueDiscrete) issue;
            ValueDiscrete value=(ValueDiscrete)bid.getValue(issue);
           
            double eval=additiveUtilitySpaceFactory.getUtility(issue, value);
            inputMatrix[i][j]= eval;
            evals.add(eval);
//            for(Value value : values.getValues()) {
//            double evaluation=additiveUtilitySpaceFactory.getUtility(issue, (ValueDiscrete)value);
//            evals.add(evaluation);
//         
//            }
            InputSpace.put(bidRanking.indexOf(bid),evals);
            j++;
        }
        i++;
        }
        RealMatrix iMatrix=MatrixUtils.createRealMatrix(inputMatrix);
        RealMatrix tMatrix=MatrixUtils.createRealMatrix(target);
 
        //Get the weights using pseudo inverse
        RealMatrix wMatrix =getPseudoInverse(iMatrix).multiply(tMatrix);
        int l=0;
        for(Issue issue: additiveUtilitySpaceFactory.getIssues()) {
        	
        	double weight=wMatrix.getRow(l)[0];
        	System.out.println("weight "+l+"="+weight);
        	additiveUtilitySpaceFactory.setWeight(issue,weight);
        	l++;
        }
     additiveUtilitySpaceFactory.normalizeWeights();
        return additiveUtilitySpaceFactory.getUtilitySpace();
    }
    private AdditiveUtilitySpaceFactory evaluationInitialize(){
    	BidRanking bidRanking =userModel.getBidRanking();
        AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory=new AdditiveUtilitySpaceFactory(userModel.getDomain());  //鐩存帴鑾峰緱褰撳墠utilitySpace涓嬬殑domain.
        additiveUtilitySpaceFactory.estimateUsingBidRanks(bidRanking);//initialize evaluation, the weights remain unknown for now
//        List<Issue> issues=Domain.getIssues();
//        for(Issue issue:issues){
//            additiveUtilitySpaceFactory.setWeight(issue,random.nextDouble());    //璁剧疆姣忎釜issue鐨勬潈閲�
//          
//            IssueDiscrete values=(IssueDiscrete) issue;       //灏唅ssue寮哄埗杞崲涓簐alues闆嗗悎
//            for (Value value:values.getValues()){            //閫氳繃values闆嗗悎锛岃幏鍙栨瘡涓獀alue銆�
//                additiveUtilitySpaceFactory.setUtility(issue,(ValueDiscrete)value,random.nextDouble());   //鍥犱负鐜板湪鏄疮鍔犳晥鐢ㄧ┖闂达紝闅忎究璁剧疆涓�涓潈閲嶄箣鍚庯紝鍙互瀵瑰綋鍓嶈繖涓獀alue璁剧疆涓�涓晥鐢紝鏁堢敤闅忔満銆�
//            }                                                                                            //褰撴晥鐢ㄧ‘瀹氫簡涔嬪悗锛屽綋鍓嶇殑value鑷繁鏈韩鐨勫�间篃灏辩‘瀹氫簡銆�
//            //杩欓噷璁剧疆鐨勬晥鐢ㄦ槸璁剧疆value鐨別valuation
//        }
//        additiveUtilitySpaceFactory.normalizeWeights(); //鍥犱负涔嬪墠瀵规瘡涓獀value鐨勬晥鐢ㄥ�艰绠楅兘鏄殢鏈虹殑锛岃繖涓椂鍊欙紝闇�瑕佸綊涓�鍖栥��
        return  additiveUtilitySpaceFactory;  
    }
}
