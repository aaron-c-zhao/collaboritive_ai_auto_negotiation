package collabai.group42.acceptance;

import collabai.group42.BoaState;
import geniusweb.issuevalue.Bid;
import geniusweb.profile.utilityspace.UtilitySpace;

public class TimeAcceptanceStrategy implements AcceptanceStrategy {
    private final double T = 0.92;
@Override
public Boolean isAcceptable(Bid bid, BoaState state) {
        double progress = state.getProgress().get(System.currentTimeMillis());
        return (progress > T - 0.000001);
        }

        }
