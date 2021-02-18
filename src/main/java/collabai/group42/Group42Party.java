package collabai.group42;


import geniusweb.actions.Vote;
import geniusweb.boa.InstantiationFailedException;
import geniusweb.boa.acceptancestrategy.AcceptanceStrategy;
import geniusweb.boa.biddingstrategy.BiddingStrategy;
import geniusweb.inform.Settings;
import geniusweb.opponentmodel.OpponentModel;
import tudelft.utilities.logging.Reporter;
import geniusweb.boa.DefaultBoa;

/**
 * A simple party that places random bids and accepts when it receives an offer
 * with sufficient utility.
 * <h2>parameters</h2>
 * <table >
 * <caption>parameters</caption>
 * <tr>
 * <td>minPower</td>
 * <td>This value is used as minPower for placed {@link Vote}s. Default value is
 * 2.</td>
 * </tr>
 * <tr>
 * <td>maxPower</td>
 * <td>This value is used as maxPower for placed {@link Vote}s. Default value is
 * infinity.</td>
 * </tr>
 * </table>
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
		return null;
	}

	@Override
	protected BiddingStrategy getBiddingStrategy(Settings settings) throws InstantiationFailedException {
		return null;
	}

	@Override
	protected AcceptanceStrategy getAccceptanceStrategy(Settings settings) throws InstantiationFailedException {
		return null;
	}


	@Override
	public String getDescription() {
		return "places random bids until it can accept an offer with utility >0.6. "
				+ "Parameters minPower and maxPower can be used to control voting behaviour.";
	}


}
