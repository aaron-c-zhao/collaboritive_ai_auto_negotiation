package collabai.group42.acceptance;

import collabai.group42.biddingStrategy.ExtendedUtilSpace;
import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.bidspace.BidsWithUtility;
import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.Profile;
import geniusweb.profile.utilityspace.LinearAdditive;
import geniusweb.profile.utilityspace.UtilitySpace;


public class PrevAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 1;
    private final double b = 0;
    //https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        Bid lastOffer = null;

        for (int i = state.getActionHistory().size() - 1; i >= 0; i--) {
            Action action = state.getActionHistory().get(i);
            if (action.getActor() == state.getSettings().getID() && action instanceof ActionWithBid);
            ActionWithBid offer = (ActionWithBid) action;
            lastOffer = offer.getBid();
        }

        UtilitySpace utilSpace = (LinearAdditive) state.getProfile();

        if (lastOffer == null) return false; // First bid

        return utilSpace.getUtility(bid).doubleValue()*a + b > utilSpace.getUtility(lastOffer).doubleValue();
    }

    // use this or just cast profile to utilitySpace
    private ExtendedUtilSpace getBidSpace(LinearAdditive profile) {
        return new ExtendedUtilSpace(profile);
    }

}
