package collabai.group42;


import collabai.group42.opponent.MyFrequencyOpponentModel;
import geniusweb.boa.InstantiationFailedException;
import collabai.group42.biddingStrategy.Group42BiddingStrategy;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.boa.acceptancestrategy.TimeDependentAcceptanceStrategy;
import geniusweb.boa.biddingstrategy.BiddingStrategy;
import geniusweb.boa.biddingstrategy.TimeDependentBiddingStrategy;
import geniusweb.inform.Settings;
import geniusweb.opponentmodel.FrequencyOpponentModel;
import geniusweb.opponentmodel.OpponentModel;
import tudelft.utilities.logging.Reporter;
import geniusweb.boa.DefaultBoa;

/**
 * An implementation of the BOA party in negotiation. The party only supports SOAP protocol, and is only capable of
 * bilateral negotiations. Each negotiation session is expected to last for 200 rounds.
 *
 * This is a hardcoded BOA client, no settings file required.
 */
public class Group42Party extends DefaultBoa {

	public Group42Party() {
		super();
	}

	public Group42Party(Reporter reporter) {
		super(reporter); // for debugging
	}

	@Override
	protected Class<? extends OpponentModel> getOpponentModel(Settings settings) throws InstantiationFailedException {
		return MyFrequencyOpponentModel.class;
	}

	@Override
	protected BiddingStrategy getBiddingStrategy(Settings settings){
		return new Group42BiddingStrategy();
	}

	@Override
	protected AcceptanceStrategy getAccceptanceStrategy(Settings settings) throws InstantiationFailedException {
		return new TimeDependentAcceptanceStrategy();
	}


	@Override
	public String getDescription() {
		return "places random bids until it can accept an offer with utility >0.6. "
				+ "Parameters minPower and maxPower can be used to control voting behaviour.";
	}


}
