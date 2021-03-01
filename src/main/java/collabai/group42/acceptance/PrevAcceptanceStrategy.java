package collabai.group42.acceptance;

import geniusweb.actions.Action;
import geniusweb.actions.Offer;
import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;


public class PrevAcceptanceStrategy implements AcceptanceStrategy {
    private final double a = 1.02;
    private final double b = 0;
    //https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf

    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        Bid lastOffer = null;

        for (int i = state.getActionHistory().size() - 1; i >= 0; i--) {
            Action action = state.getActionHistory().get(i);
            if (action.getActor() == state.getSettings().getID() && action instanceof Offer);
            Offer offer = (Offer) action;
            lastOffer = offer.getBid();
        }

        UtilitySpace utilSpace = (UtilitySpace) state.getProfile();

        if (lastOffer == null) return false; // First bid

        return utilSpace.getUtility(bid).doubleValue()*a + b > utilSpace.getUtility(lastOffer).doubleValue();
    }

}
