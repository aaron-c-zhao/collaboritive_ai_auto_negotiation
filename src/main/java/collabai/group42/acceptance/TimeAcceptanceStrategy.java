package collabai.group42.acceptance;

import geniusweb.boa.BoaState;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.issuevalue.Bid;

public class TimeAcceptanceStrategy implements AcceptanceStrategy {
    private final double T = 0.92;
@Override
public Boolean isAcceptable(Bid bid, BoaState state) {
        double progress = state.getProgress().get(System.currentTimeMillis());
        return (progress > T - 0.000001);
        }

        }
