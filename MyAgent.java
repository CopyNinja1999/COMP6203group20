package group20;

import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.Domain;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.EndNegotiation;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.uncertainty.UserModel;
import genius.core.utility.*;
import group20.GeneticEstimatesUtility;
/**
 * A simple example agent that makes random bids above a minimum target utility.
 *
 * @author Tim Baarslag
 */
public class MyAgent extends AbstractNegotiationParty
{
	private static double MAXIMUM_UTILITY;
	private static double MINIMUM_UTILITY;
	private double concedeThreshold;
	private static double rankThreshold;
	private Bid lastOffer;
	private HashMap<Integer, HashMap<String, Integer>> frequencyTable = new HashMap<>();
	private int noBids = 0;
	private AdditiveUtilitySpace additiveUtilitySpace;

	/**
	 * Initializes a new instance of the agent.
	 */
	@Override
	public void init(NegotiationInfo info)
	{
		super.init(info);

		if (hasPreferenceUncertainty()) {
			GeneticEstimatesUtility genetic=new GeneticEstimatesUtility(userModel);
			AbstractUtilitySpace abstractUtilitySpace=genetic.geneticUtilitySpace();
			BidRanking bidRanking = userModel.getBidRanking();
			System.out.println("HAS PREFERENCE UNCERTAINTY!!!");
			System.out.println("Agent ID: " + info.getAgentID());
			System.out.println("No. of possible bids in domain: " + userModel.getDomain().getNumberOfPossibleBids());
			System.out.println("No. of bids in preference ranking: " + bidRanking.getSize());
			System.out.println("Elicitation cost: " + info.getUser().getElicitationCost());
			System.out.println("Lowest utility bid: " + bidRanking.getMinimalBid());
			System.out.println("Highest utility bid: " + bidRanking.getMaximalBid());
			System.out.println("5th bid in ranking list: " + bidRanking.getBidOrder().get(4));
			MAXIMUM_UTILITY=estimateUtilitySpace().getUtility(userModel.getBidRanking().getMaximalBid());//get maximum utility bid based on the estimate function
			MINIMUM_UTILITY=estimateUtilitySpace().getUtility(userModel.getBidRanking().getMinimalBid());
			System.out.println("Max utility= "+MAXIMUM_UTILITY+"Minimum utility="+MINIMUM_UTILITY);
		}

		AbstractUtilitySpace utilitySpace = info.getUtilitySpace();
		additiveUtilitySpace = (AdditiveUtilitySpace) utilitySpace;

		List<Issue> issues = additiveUtilitySpace.getDomain().getIssues();

		for (Issue issue : issues) {
			int issueNumber = issue.getNumber();
			System.out.println(">> " + issue.getName() + " weight: " + additiveUtilitySpace.getWeight(issueNumber));

			// Assuming that issues are discrete only
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			EvaluatorDiscrete evaluatorDiscrete = (EvaluatorDiscrete) additiveUtilitySpace.getEvaluator(issueNumber);

			HashMap<String, Integer> issueHashMap = new HashMap<>();

			for (ValueDiscrete valueDiscrete : issueDiscrete.getValues()) {
				String value = valueDiscrete.getValue();
				issueHashMap.put(value, 0);
				System.out.println(value);
				System.out.println("Evaluation(getValue): " + evaluatorDiscrete.getValue(valueDiscrete));
				try
				{
					System.out.println("Evaluation(getEvaluation): " + evaluatorDiscrete.getEvaluation(valueDiscrete));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			frequencyTable.put(issueNumber, issueHashMap);

			rankThreshold = 0;
		}
	}

	/**
	 * Makes a random offer above the minimum utility target
	 * Accepts everything above the reservation value at the very end of the negotiation; or breaks off otherwise.
	 */
	@Override
	public Action chooseAction(List<Class<? extends Action>> possibleActions)
	{
		// Check for acceptance if we have received an offer

		if (lastOffer != null)
		{
			rankThreshold = timeline.getTime();//start from 0 and ended up with 1
			System.out.println("Current threshold: " + rankThreshold);
			System.out.println();
			//TODO：update accpet strategy
			if (timeline.getTime() >= 0.99)
			{
				if (isRankAboveThreshold(lastOffer))
					return new Accept(getPartyId(), lastOffer);
				else
					return new EndNegotiation(getPartyId());
			}
			else if (isRankAboveThreshold(lastOffer))
			{
				return new Accept(getPartyId(), lastOffer);
			}
		}
		// first offer
		// Otherwise, send out a random offer above the target utility
		return new Offer(getPartyId(), getRandomBidAboveThreshold());
	}

	/**
	 * Check if the rank of offer is above the threshold.
	 * Elicit the offer if needed
	 * @param bid
	 * @return
	 */
	private boolean isRankAboveThreshold(Bid bid)
	{
		// Check if bid is in current ranking
		BidRanking bidRanking = userModel.getBidRanking();
		List<Bid> bidOrder = bidRanking.getBidOrder();
		if (bidOrder.contains(bid)) {
			// True if above rank, false otherwise
			int noRanks = bidRanking.getSize();
			System.out.println("No. of ranks: " + noRanks);
			int rank = noRanks - bidRanking.indexOf(bid); // Highest index is ranked best
			System.out.println("Rank of bid: " + rank);
			boolean result = rank <= (noRanks * rankThreshold);
			System.out.println("Within threshold? " + result);
			return result;
		}
		//Zheyu.Zhang
//		else{
//			userModel=user.elicitRank(bid,userModel);
//
//		}
		System.out.print("bother="+user.getTotalBother());
		// Elicit the bid rank from the user, and update the userModel
		userModel = user.elicitRank(bid, userModel);//add the bid to the correct ranking
		bidRanking = userModel.getBidRanking();
		int noRanks = bidRanking.getSize();
		System.out.println("No. of ranks: " + noRanks);
		int rank = noRanks - bidRanking.indexOf(bid); // Highest index is ranked best
		System.out.println("Rank of bid: " + rank);
		boolean result = rank <= (noRanks * rankThreshold);
		System.out.println("Within threshold? " + result);
		return result;
	}

//TODO: fix the strategy to maximize the Utility and Euclidean distance(Nash point)
	private Bid getRandomBidAboveThreshold() {
		BidRanking bidRanking = userModel.getBidRanking();
		int noRanks = bidRanking.getSize();
		System.out.println("No ranks: " + noRanks);
		int thresholdedRanks = (int)(noRanks * rankThreshold);
		System.out.println("Ranks within threshold: " + noRanks);
		Random rand = new Random();
		int randRank = rand.nextInt(thresholdedRanks + 1);
		System.out.println("Random rank = " + randRank);
		return bidRanking.getBidOrder().get(noRanks - randRank - 1);
	}
	private UtilitySpace getRealUtilitySpace(){
		ExperimentalUserModel e = ( ExperimentalUserModel ) userModel ;
		UncertainAdditiveUtilitySpace realUSpace = e. getRealUtilitySpace();
		return realUSpace;
	}
	private Bid getParetoOffer(int sampleSize){
		double maxOpponentUtil = 0;
		Bid approxParetoBid = null;
		double maxWelfare=0;
		for (int n = 0; n < sampleSize; n++)
		{
			Bid randomBid;
			double util;
			int i = 0;
			// try 100 times to find a bid under the target utility
				randomBid = getRandomBidAboveThreshold();
				util = estimateUtilitySpace().getUtility(randomBid);
				// Estimate opponent utility
			double opponentUtil = predictValuation(randomBid);
			if (opponentUtil>maxOpponentUtil){
				maxOpponentUtil=opponentUtil;
				approxParetoBid=randomBid;
			}
		}

		return approxParetoBid;


	}
	private double square(double a){
		return a*a;
	}
	private double calculateEuclideanDistanceUtilitySpace(AbstractUtilitySpace U1, UtilitySpace U2) {
		double distance = 0.0D;
//TODO: implement Euclidean distance
//		try {
//			for(int i = 0; i < var1.length; ++i) {
//				distance += this.square(var2[i] - var1[i]);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		distance /= (double)this.utilitySpace.getDomain().getNumberOfPossibleBids();
		return distance;
	}
	/**
	 * Remembers the offers received by the opponent.
	 */
	@Override
	public void receiveMessage(AgentID sender, Action action)
	{
		if (action instanceof Offer)
		{
			lastOffer = ((Offer) action).getBid();

			System.out.println("Received offer: " + lastOffer.toString());
			noBids += 1;
			// Update frequency table
			List<Issue> issues = lastOffer.getIssues();

			for (Issue issue : issues) {
				int issueNumber = issue.getNumber();
				String value = ((ValueDiscrete) lastOffer.getValue(issueNumber)).getValue();
				int currentCount = frequencyTable.get(issueNumber).get(value);
				frequencyTable.get(issueNumber).put(value, currentCount + 1);
			}

			printFrequencyTable();
			double predictedValue = predictValuation(lastOffer);
			System.out.println("Predicted value: " + predictedValue);
		}
	}

	/**
	 * Predict the valuation of an offer for an opponent.
	 * @param offer The offer the opponent has made
	 * @return The predicted utility value of the opponent
	 * TODO: returns an UtilitySpace
	 */
	private double predictValuation(Bid offer)
	{
		List<Issue> issues = offer.getIssues();
		double value = 0;
		double[] optionValues = getOptionValues(offer, issues);
		double[] normalisedWeights = getNormalisedWeights(issues);
		for (int i = 0; i < optionValues.length; i++)
			value += optionValues[i] * normalisedWeights[i];

		return value;
	}

	/**
	 * Calculate the value of an opponent's options
	 * @param bid The bid provided by the opponent
	 * @param issues A list of issues that the bid negotiates about
	 * @return A list of option values, calculated using preference order
	 */
	private double[] getOptionValues(Bid bid, List<Issue> issues) {
		double[] optionValues = new double[issues.size()];
		for (int i = 0; i < issues.size(); i++)
		{
			Issue issue = issues.get(i);
			int issueNumber = issue.getNumber();
			HashMap<String, Integer> options = frequencyTable.get(issueNumber);
			double noOptions = options.keySet().size();

			String chosenOption = ((ValueDiscrete) bid.getValue(issueNumber)).getValue();
			int rank = 0;
			int optionValue = options.get(chosenOption);
			for (String option : options.keySet())
			{
				if (options.get(option) >= optionValue)
					rank += 1;
			}

			optionValues[i] = (noOptions - rank + 1) / noOptions;
		}

		return optionValues;
	}

	/**
	 * Get the normalised weights for each issue, using the Gini Index
	 * @param issues A list of issues that the bid negotiates about
	 * @return An array of weights
	 */
	private double[] getNormalisedWeights(List<Issue> issues) {
		double[] weights = new double[issues.size()];
		double noBidsSquared = Math.pow(noBids, 2);

		for (int i = 0; i < issues.size(); i++)
		{
			List<Integer> optionCounts = new ArrayList<>(frequencyTable.get(issues.get(i).getNumber()).values());
			double weight = 0;
			for (Integer option : optionCounts)
				weight += (Math.pow(option, 2) / noBidsSquared);

			weights[i] = weight;
		}

		double weightSum = Arrays.stream(weights).sum();
		double[] normalisedWeights = new double[issues.size()];
		for (int i = 0; i < weights.length; i++)
			normalisedWeights[i] = weights[i] / weightSum;

		return normalisedWeights;
	}

	private void printFrequencyTable() {
		System.out.println("Frequency table");
		for (Integer issueNo : frequencyTable.keySet()) {
			System.out.print("Issue" + issueNo + " ");
			for (String option : frequencyTable.get(issueNo).keySet()) {
				System.out.print(frequencyTable.get(issueNo).get(option) + " ");
			}
			System.out.println();
		}
	}

	@Override
	public String getDescription()
	{
		return "Based on the Johny Black Agent";
	}
	//
public AbstractUtilitySpace estimateUtilitySpaceFromExample(){
	AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(this.getDomain());
	List issues = additiveUtilitySpaceFactory.getIssues();
	Iterator iterator = issues.iterator();

	while(iterator.hasNext()) {
		IssueDiscrete issueDiscrete = (IssueDiscrete)iterator.next();
		additiveUtilitySpaceFactory.setWeight(issueDiscrete, 1.0D / (double)issues.size());
		Iterator nextIter = issueDiscrete.getValues().iterator();

		while(nextIter.hasNext()) {
			ValueDiscrete valueDiscrete = (ValueDiscrete)nextIter.next();
			int value = 0;
			Iterator bidIterator = this.userModel.getBidRanking().getBidOrder().iterator();

			while(bidIterator.hasNext()) {
				Bid var9 = (Bid)bidIterator.next();
				if (var9.containsValue(issueDiscrete, valueDiscrete)) {
					++value;
				}
			}

			additiveUtilitySpaceFactory.setUtility(issueDiscrete, valueDiscrete, (double)value);
		}
	}

	additiveUtilitySpaceFactory.scaleAllValuesFrom0To1();
	return additiveUtilitySpaceFactory.getUtilitySpace();
}
//TODO: need improvement
	/**
	 * A simple heuristic for estimating a discrete {@link AdditiveUtilitySpace} from a {@link BidRanking}.
	 * Gives 0 points to all values occuring in the lowest ranked bid,
	 * then 1 point to all values occuring in the second lowest bid, and so on.
	 */
	@Override
	public AbstractUtilitySpace estimateUtilitySpace()
	{
		AdditiveUtilitySpace utilitySpace = setupUtilitySpace();

		BidRanking bidRanking = userModel.getBidRanking();
		double points = 0;
		for (Bid bid : bidRanking.getBidOrder())
		{
			List<Issue> issues = bid.getIssues();
			for (Issue issue : issues)
			{
				EvaluatorDiscrete evaluator = (EvaluatorDiscrete) utilitySpace.getEvaluator(issue);
				int issueNo = issue.getNumber();
				ValueDiscrete value = (ValueDiscrete) bid.getValue(issueNo);
				double oldUtil = evaluator.getDoubleValue(value);
				evaluator.setEvaluationDouble(value, oldUtil + points);
			}
			points += 1;
		}
		normaliseWeightsByMaxValues(utilitySpace);
		return utilitySpace;
	}
//Still Jonny black frequency table
	private AdditiveUtilitySpace setupUtilitySpace()
	{
		Domain domain = userModel.getDomain();
		List<Issue> issues = domain.getIssues();
		int noIssues = issues.size();
		Map<Objective, Evaluator> evaluatorMap = new HashMap<>();
		for (Issue issue : issues)
		{
			IssueDiscrete issueDiscrete = (IssueDiscrete) issue;
			EvaluatorDiscrete evaluator = new EvaluatorDiscrete();
			evaluator.setWeight(1.0 / noIssues);
			for (ValueDiscrete value : issueDiscrete.getValues())
				evaluator.setEvaluationDouble(value, 0.0);

			evaluatorMap.put(issue, evaluator);
		}

		return new AdditiveUtilitySpace(domain, evaluatorMap);
	}

	private void normaliseWeightsByMaxValues(AdditiveUtilitySpace utilitySpace)
	{
		for (Issue issue : utilitySpace.getDomain().getIssues())
		{
			EvaluatorDiscrete evaluator = (EvaluatorDiscrete) utilitySpace.getEvaluator(issue);
			evaluator.normalizeAll();
		}
		scaleAllValueFrom0To1(utilitySpace);
		utilitySpace.normalizeWeights();
	}

	private void scaleAllValueFrom0To1(AdditiveUtilitySpace utilitySpace)
	{
		for (Issue issue : utilitySpace.getDomain().getIssues())
		{
			EvaluatorDiscrete evalutor = (EvaluatorDiscrete) utilitySpace.getEvaluator(issue);
			evalutor.scaleAllValuesFrom0To1();;
		}
	}

}
