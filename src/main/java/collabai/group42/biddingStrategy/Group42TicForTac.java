package collabai.group42.biddingStrategy;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.boa.BoaState;
import geniusweb.issuevalue.Bid;
import tudelft.utilities.immutablelist.ImmutableList;

import java.math.BigDecimal;

public class Group42TicForTac extends Group42BiddingStrategy {

    private double[] nashPoint = {0.0, 0.0};
    private double reserValue = getMin();
    private double maxValue = 1.0;
    private static final int STEP_NUM = 100;


    @Override
    public Action getAction(BoaState boaState) {
        if (this.bidSpace == null) {
            init(boaState);
        }

        Bid lastBid = getLastBid(boaState.getActionHistory());

        double targetUtility = getTargetUtility(
                boaState.getProgress().get(System.currentTimeMillis()), lastBid);

        ImmutableList<Bid> bidOptions = bidSpace
                .getBids((BigDecimal.valueOf(targetUtility)));

        if (bidOptions.size().intValue() == 0)
            getAlterAction(boaState, targetUtility, lastBid, bidOptions);

        return new Offer(me, getNiceBid(bidOptions));
    }


    protected double getTargetUtility(Double progress, Bid lastBid) {
        if (progress < 0.1)
            return 1 - progress;
        else if (progress < 0.99){
            updateNashPoint();
            double dist = getOpponentUtility(lastBid) / nashPoint[1];
            dist = Math.min(dist, 1.0);
            return 1.0 - dist* (1.0 - nashPoint[0]);
        }
        else return getMin();
    }

    /**
     * Update the nash point according to the most recent opponent model.
     */
    private void updateNashPoint() {
        double maxProduct = 0.0;
        double step = (maxValue - reserValue) / STEP_NUM;
        for (int i = STEP_NUM; i > 0; i--) {
            double profit = i * step;
            double utility = reserValue + profit;
            ImmutableList<Bid> bidOptions = bidSpace
                .getBids((BigDecimal.valueOf(utility)));
            if (bidOptions.size().intValue() == 0) continue;
            Bid bid = getNiceBid(bidOptions);
            double utilityOp = getOpponentUtility(bid);
            double product = (utilityOp - getOpponentReservation())
                  * profit;
            if (maxProduct < product) {
                nashPoint[0] = utility;
                nashPoint[1] = utilityOp;
            }
        }
    }


    /**
     * Retrieve the opponent's reservation value from the opponent model.
     *
     * Could be set to a fix value or the same value as the agent itself if the opponent model
     * does not support reservation value estimation.
     *
     * @return opponent's reservation value.
     */
    private double getOpponentReservation() {
        return 0.0;
    }

}
