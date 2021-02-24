package collabai.group42.acceptance;

import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

public class TimeAcceptanceStrategy implements AcceptanceStrategy {
    //https://homepages.cwi.nl/~baarslag/pub/Acceptance_conditions_in_automated_negotiation.pdf
    @Override
    public Boolean isAcceptable(Bid bid, BoaState state) {
        double progress = state.getProgress().get(System.currentTimeMillis());
        return (progress > 1 - 0.000001);
    }

}
