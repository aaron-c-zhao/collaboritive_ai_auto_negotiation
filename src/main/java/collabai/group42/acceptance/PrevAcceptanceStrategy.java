package collabai.group42.acceptance;

import geniusweb.actions.Action;
import geniusweb.actions.ActionWithBid;
import geniusweb.actions.Offer;
import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;


public class PrevAcceptanceStrategy implements AcceptanceStrategy {
    //https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf
    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        double progress = state.getProgress().get(System.currentTimeMillis());

        Bid lastBid = null;

        for (Action action : state.getActionHistory()) {
            if (action.getActor() == state.getSettings().getID() && action instanceof ActionWithBid);
            ActionWithBid offer = (ActionWithBid) action;
            lastBid = offer.getBid();
        }

        new UtilitySpace(state.getSettings().getProfile());


        return (state.getProfile() > 1 - 0.000001);
    }

}
