package collabai.group42.acceptance;

import collabai.group42.biddingStrategy.ExtendedUtilSpace;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;


//https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf

public class NextAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 1.02;
    private final double b = 0;
    private Bid nextBid = null;

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        Bid nextOffer = null;

        if (nextBid == null) throw new IllegalArgumentException("Next bid is null");

        UtilitySpace utilSpace = (UtilitySpace) state.getProfile();

        return (utilSpace.getUtility(bid).doubleValue() * a + b > utilSpace.getUtility(nextOffer).doubleValue());
    }

    public void setNextBid(Bid bid) {
        this.nextBid = bid;
    }
}
