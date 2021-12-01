package group20;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.EvaluatorDiscrete;

import java.util.ArrayList;
import java.util.List;

public class ModifiedAdditiveUtilitySpaceFactory extends AdditiveUtilitySpaceFactory
{
    private AdditiveUtilitySpace u=getUtilitySpace();

    /**
     * Generates an simple Utility Space on the domain, with equal weights and zero values.
     * Everything is zero-filled to already have all keys contained in the utility maps.
     */
    public ModifiedAdditiveUtilitySpaceFactory(Domain d)
    {
        super(d);
    }
    public void estimateUsingBidRanks(BidRanking bidRanking, Integer n) {
        // list of bids
        List<Bid> bidOrder = bidRanking.getBidOrder();

        // find the number of issues
        Integer numberOfIssues = bidOrder.get(0).getIssues().size();
        Double coefficient = numberOfIssues.doubleValue();

        // number of bids in bidOrder
        Integer numOfBids = bidOrder.size();
        //this.numberOfBids = numberOfBids;

        Integer bidRank = 1; // initialise the rank we assign to each bid

        // Initialise the points
        Double points = coefficient * bidRank;
        for (Integer bidIndex = 0; bidIndex < numOfBids; bidIndex++) {
            ArrayList<Integer> frequencyOfValues = countValues(bidOrder, bidIndex, n);

            // find the sum of the frequencies
            Double sumOfFrequencies = 0.0;
            for (Integer f : frequencyOfValues) {
                sumOfFrequencies += f.doubleValue();
            }

            // find the fraction of points we assign to each value
            ArrayList<Double> newPoints = new ArrayList<>(); // store the number of points for each value
            for (Integer f : frequencyOfValues) {
                newPoints.add((f.doubleValue() / sumOfFrequencies) * points);
            }

            // Update the utility of each value
            List<Issue> issues = bidOrder.get(bidIndex).getIssues();
            Bid bid = bidOrder.get(bidIndex);
            for (Issue issue : issues) {
                Integer issueNumber = issue.getNumber();
                ValueDiscrete v = (ValueDiscrete) bid.getValue(issueNumber);

                Double oldUtility = getUtility(issue, v);
                Double newUtility = oldUtility + newPoints.get(issueNumber - 1);

                setUtility(issue, v, newUtility);
            }

            bidRank++;
            points = coefficient * bidRank; // update the points we distribute throughout each issue
            normalizeWeightsByMaxValues();
        }
    }
    private void normalizeWeightsByMaxValues()
    {
        for (Issue i : getIssues())
        {
            EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
            evaluator.normalizeAll();
        }
        scaleAllValuesFrom0To1();
        u.normalizeWeights();
    }
    /**
     * Returns the utility space that has been created.
     */

    private ArrayList<Integer> countValues(List<Bid> bidOrder, Integer bidIndex, Integer n) {
        // this method takes each value in the bid at bidIndex in the bidOrder list, and counts how many bids have
        // the same values in the range of bids [bidIndex - n, bidIndex + n].
        Bid bid = bidOrder.get(bidIndex);
        Integer numOfBids = bidOrder.size();

        ArrayList<Integer> counterList = new ArrayList<>();

        // for bids in the range [bidIndex - n, bidIndex + n], we calculate the number of times the same values occurs
        List<Issue> issues = bid.getIssues();
        for (Issue issue : issues) {
            Integer issueNumber = issue.getNumber();

            ValueDiscrete valueDiscrete = (ValueDiscrete) bid.getValue(issueNumber);

            // initialise the counter
            int counter = 1; // start at one to include valueDiscrete

            // compareIndex is the index of the bid which we compare the values against the bid at bidIndex
            for (Integer compareIndex = bidIndex - n; compareIndex <= bidIndex + n; compareIndex += 1) {
                if (!compareIndex.equals(bidIndex) &&
                        compareIndex >= 0 &&      // ensure compareIndex is within the size of bifOrder
                        compareIndex <= numOfBids - 1) { // ensure compareIndex is within the size of bifOrder

                    Bid compareBid = bidOrder.get(compareIndex); // get the bid which we compare the values to bid

                    ValueDiscrete compareValueDiscrete = (ValueDiscrete) compareBid.getValue(issueNumber);
                    if (valueDiscrete.equals(compareValueDiscrete)) {
                        counter += 1;
                    }
                }
            }
            counterList.add(counter);
        }
        return counterList;
    }

}

